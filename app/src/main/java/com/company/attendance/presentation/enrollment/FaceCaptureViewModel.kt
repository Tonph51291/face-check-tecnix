package com.company.attendance.presentation.enrollment

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.logging.logD
import com.company.attendance.core.logging.logE
import com.company.attendance.core.logging.logI
import com.company.attendance.core.result.Result
import com.company.attendance.core.util.toBase64Jpeg
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.model.FaceAlignmentStatus
import com.company.attendance.domain.model.FaceDetectionData
import com.company.attendance.domain.model.FaceQualityMetrics
import com.company.attendance.domain.repository.EmployeeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import com.company.attendance.domain.ml.ClearanceDecision
import com.company.attendance.domain.ml.FaceClearanceGatekeeper

/**
 * Hiệu ứng một lần (One-off Navigation & UI Effects) cho màn hình FaceCapture
 */
sealed interface FaceCaptureEffect {
    data class NavigateToReview(
        val bitmap: Bitmap?,
        val metrics: FaceQualityMetrics
    ) : FaceCaptureEffect
    data object NavigateBack : FaceCaptureEffect
}

/**
 * ViewModel điều khiển quy trình Smart Auto-Capture & Multi-shot Face Enrollment (3 mẫu).
 * Tích hợp tự động nhận diện ổn định (Face Stability Detection), chụp tự động và gửi Base64 lên AI Server.
 * Tích hợp FaceClearanceGatekeeper để chặn hoàn toàn việc spam request khi bị trùng mặt 409 cho đến khi đổi người.
 * Điều hướng được phát qua Channel/Flow effect để tách biệt hoàn toàn khỏi Compose UI.
 */
class FaceCaptureViewModel(
    private val employeeRepository: EmployeeRepository = RepositoryModule.employeeRepository
) : ViewModel() {

    private val clearanceGatekeeper = FaceClearanceGatekeeper()

    private val _uiState = MutableStateFlow(FaceCaptureUiState())
    val uiState: StateFlow<FaceCaptureUiState> = _uiState.asStateFlow()

    private val _effect = Channel<FaceCaptureEffect>(Channel.BUFFERED)
    val effect: Flow<FaceCaptureEffect> = _effect.receiveAsFlow()

    private var bitmapProvider: (() -> Bitmap?)? = null
    private var cooldownJob: Job? = null
    private var isCoolingDown = false
    private var latestFaceData: FaceDetectionData? = null

    /**
     * Khởi tạo hoặc đặt lại hoàn toàn trạng thái đăng ký cho nhân viên để bắt đầu phiên chụp mới.
     */
    fun resetEnrollment(employeeId: String) {
        if (employeeId.isBlank()) return
        cooldownJob?.cancel()
        isCoolingDown = false
        clearanceGatekeeper.reset()
        _uiState.update {
            it.copy(
                employeeId = employeeId,
                currentStep = 1,
                sampleCount = 0,
                stabilityProgress = 0f,
                errorMessage = null,
                lastCapturedBitmap = null,
                isUploading = false,
                isFaceAligned = false,
                isBlockedDuplicate = false,
                guidanceText = "Nhìn thẳng vào camera và giữ yên khuôn mặt",
                distanceText = "Khoảng cách hợp lệ (65 cm) • Độ sáng: Tốt"
            )
        }
        logI("Đặt lại (reset) phiên FaceCapture cho nhân viên: $employeeId")
    }

    fun initEmployee(employeeId: String, forceReset: Boolean = false) {
        if (employeeId.isNotBlank()) {
            if (forceReset || _uiState.value.employeeId != employeeId) {
                resetEnrollment(employeeId)
            }
        }
    }

    /**
     * Hủy phiên đăng ký và gửi sự kiện quay về màn hình trước
     */
    fun onCancelClicked() {
        resetEnrollment(_uiState.value.employeeId)
        viewModelScope.launch {
            _effect.send(FaceCaptureEffect.NavigateBack)
        }
    }

    fun setBitmapProvider(provider: () -> Bitmap?) {
        this.bitmapProvider = provider
    }

    /**
     * Xử lý từng frame từ ML Kit thời gian thực.
     * Tích hợp kiểm soát đổi người (FaceClearanceGatekeeper) để chặn đứng việc gửi request lặp lại nếu trùng mặt.
     */
    fun onFaceFrame(faceData: FaceDetectionData?) {
        this.latestFaceData = faceData
        val state = _uiState.value
        if (state.isUploading) {
            return
        }

        // 1. Kiểm tra quyết định từ FaceClearanceGatekeeper (Thuật toán tách biệt khỏi UI)
        val clearanceDecision = clearanceGatekeeper.evaluate(faceData)
        when (clearanceDecision) {
            is ClearanceDecision.Blocked -> {
                _uiState.update {
                    it.copy(
                        isFaceAligned = false,
                        stabilityProgress = 0f,
                        isBlockedDuplicate = true,
                        guidanceText = clearanceDecision.reason,
                        distanceText = "Chặn khuôn mặt đã tồn tại"
                    )
                }
                return
            }
            is ClearanceDecision.Cleared -> {
                _uiState.update {
                    it.copy(
                        isBlockedDuplicate = false,
                        errorMessage = null,
                        guidanceText = clearanceDecision.message,
                        distanceText = "Sẵn sàng đón người mới"
                    )
                }
            }
            ClearanceDecision.Allowed -> {
                if (state.isBlockedDuplicate) {
                    _uiState.update { it.copy(isBlockedDuplicate = false) }
                }
            }
        }

        if (isCoolingDown) {
            return
        }

        // 2. Không phát hiện khuôn mặt
        if (faceData == null) {
            _uiState.update {
                it.copy(
                    isFaceAligned = false,
                    stabilityProgress = 0f,
                    guidanceText = "Vui lòng nhìn vào khung ngắm camera",
                    distanceText = "Chờ khuôn mặt..."
                )
            }
            return
        }

        // 3. Khuôn mặt chưa căn chỉnh chuẩn theo oval
        if (faceData.alignmentStatus != FaceAlignmentStatus.ALIGNED) {
            _uiState.update {
                it.copy(
                    isFaceAligned = false,
                    stabilityProgress = 0f,
                    guidanceText = faceData.guidanceMessage,
                    distanceText = "Chưa căn chuẩn"
                )
            }
            return
        }

        // 3. Kiểm tra tiêu chí chi tiết theo từng bước chụp
        val stepMatched = when (state.currentStep) {
            1 -> abs(faceData.headEulerAngleY) < 10f && abs(faceData.headEulerAngleX) < 10f
            2 -> abs(faceData.headEulerAngleX) < 14f // Góc tự nhiên hoặc nghiêng nhẹ
            3 -> (faceData.leftEyeOpenProbability ?: 1f) > 0.6f && (faceData.rightEyeOpenProbability ?: 1f) > 0.6f
            else -> true
        }

        if (!stepMatched) {
            val stepGuidance = when (state.currentStep) {
                1 -> "Vui lòng quay mặt nhìn thẳng vào giữa khung oval"
                2 -> "Giữ đầu tự nhiên, không ngửa quá cao"
                3 -> "Mở mắt tự nhiên và nhìn vào camera"
                else -> "Giữ yên khuôn mặt"
            }
            _uiState.update {
                it.copy(
                    isFaceAligned = true,
                    stabilityProgress = 0f,
                    guidanceText = stepGuidance,
                    distanceText = "Khoảng cách hợp lệ (65 cm)"
                )
            }
            return
        }

        // 4. Khuôn mặt hoàn toàn hợp lệ -> Tăng tiến độ ổn định (Stability Accumulation ~1.2s)
        val newProgress = min(state.stabilityProgress + 0.12f, 1.0f)
        val progressPercent = (newProgress * 100).toInt()

        _uiState.update {
            it.copy(
                isFaceAligned = true,
                stabilityProgress = newProgress,
                guidanceText = "Rất tốt! Giữ yên khuôn mặt... $progressPercent%",
                distanceText = "Khoảng cách chuẩn (65 cm) • Độ sáng: Tốt"
            )
        }

        // Đạt 100% ổn định -> Kích hoạt TỰ ĐỘNG CHỤP KHUÔN MẶT
        if (newProgress >= 1.0f) {
            logD("Đạt độ ổn định 100% cho Bước ${state.currentStep}/3. Tự động chụp...")
            triggerAutoCapture()
        }
    }

    /**
     * Kích hoạt tự động chụp mẫu khi khuôn mặt đạt 100% độ ổn định
     */
    private fun triggerAutoCapture() {
        val state = _uiState.value
        if (state.isUploading) return

        val bitmap = bitmapProvider?.invoke()
        if (bitmap == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Không thể lấy hình ảnh từ camera. Vui lòng thử lại.",
                    stabilityProgress = 0f
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isUploading = true,
                errorMessage = null,
                guidanceText = "Đang gửi mẫu khuôn mặt lên AI Service..."
            )
        }

        viewModelScope.launch {
            try {
                // Phân tích chất lượng ảnh thực tế: độ sắc nét, ánh sáng, góc nhìn
                val qualityMetrics = com.company.attendance.domain.ml.BitmapQualityAnalyzer.analyze(bitmap, latestFaceData)

                // Nén JPEG và mã hóa Base64
                val base64 = bitmap.toBase64Jpeg(quality = 85, maxDimension = 1080)
                val empId = state.employeeId

                val result = employeeRepository.enrollFaceSample(
                    employeeId = empId,
                    imageBase64 = base64,
                    sampleIndex = state.currentStep
                )
                when (result) {
                    is Result.Success -> {
                        val enrollData = result.data
                        val newSampleCount = enrollData.sampleCount
                        val isDone = enrollData.hasFaceSample || newSampleCount >= (enrollData.requiredSamples.takeIf { it > 0 } ?: 3)
                        val serverMsg = enrollData.message?.takeIf { it.isNotBlank() }

                        logI("Đăng ký mẫu thành công: $newSampleCount/${enrollData.requiredSamples}, Hoàn tất: $isDone. Server: $serverMsg")

                        if (isDone) {
                            _uiState.update {
                                it.copy(
                                    isUploading = false,
                                    sampleCount = newSampleCount,
                                    currentStep = 3,
                                    stabilityProgress = 1f,
                                    lastCapturedBitmap = bitmap,
                                    lastQualityMetrics = qualityMetrics,
                                    guidanceText = serverMsg ?: "Đã hoàn tất đăng ký đủ 3 mẫu khuôn mặt cho nhân viên"
                                )
                            }
                            _effect.send(FaceCaptureEffect.NavigateToReview(bitmap, qualityMetrics))
                        } else {
                            val nextStep = min(newSampleCount + 1, 3)
                            _uiState.update {
                                it.copy(
                                    isUploading = false,
                                    sampleCount = newSampleCount,
                                    currentStep = nextStep,
                                    stabilityProgress = 0f,
                                    lastCapturedBitmap = bitmap,
                                    lastQualityMetrics = qualityMetrics,
                                    guidanceText = serverMsg ?: "Đã lưu mẫu khuôn mặt số $newSampleCount thành công ($newSampleCount/3)"
                                )
                            }

                            // Khoảng thời gian đệm ngắn giữa các bước chụp
                            startCooldown(800)
                        }
                    }

                    is Result.Error -> {
                        val errorMsg = result.error.message.ifBlank { "Máy chủ từ chối mẫu khuôn mặt. Vui lòng thử lại." }
                        val isConflict = result.error is com.company.attendance.core.result.AppError.EmployeeAlreadyEnrolled ||
                                errorMsg.contains("đã được đăng ký", ignoreCase = true) ||
                                errorMsg.contains("trùng với", ignoreCase = true) ||
                                errorMsg.contains("Conflict", ignoreCase = true) ||
                                errorMsg.contains("409")

                        logE("Lỗi đăng ký mẫu (isConflict=$isConflict): $errorMsg")

                        if (isConflict) {
                            // Chặn cứng thông qua Gatekeeper (Thuật toán kiểm soát đổi người)
                            clearanceGatekeeper.blockFace(
                                trackingId = latestFaceData?.trackingId,
                                reason = errorMsg
                            )
                            _uiState.update {
                                it.copy(
                                    isUploading = false,
                                    stabilityProgress = 0f,
                                    isFaceAligned = false,
                                    isBlockedDuplicate = true,
                                    errorMessage = errorMsg,
                                    guidanceText = errorMsg,
                                    distanceText = "Vui lòng rời khỏi camera để đổi người"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isUploading = false,
                                    stabilityProgress = 0f,
                                    errorMessage = errorMsg,
                                    guidanceText = errorMsg
                                )
                            }
                            startCooldown(1200)
                        }
                    }
                }
            } catch (e: Exception) {
                logE("Ngoại lệ khi gửi mẫu khuôn mặt", e)
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        stabilityProgress = 0f,
                        errorMessage = e.localizedMessage ?: "Lỗi kết nối",
                        guidanceText = "Lỗi kết nối AI Service. Vui lòng thử lại."
                    )
                }
                startCooldown(1200)
            }
        }
    }

    private fun startCooldown(durationMs: Long) {
        isCoolingDown = true
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            delay(durationMs)
            isCoolingDown = false
        }
    }

    fun dismissError() {
        clearanceGatekeeper.reset()
        _uiState.update {
            it.copy(
                errorMessage = null,
                isBlockedDuplicate = false,
                guidanceText = "Nhìn thẳng vào camera và giữ yên khuôn mặt"
            )
        }
    }

    /**
     * Xóa các mẫu cũ trên server (DELETE /api/v1/employees/{id}/face-samples) để đăng ký lại từ đầu
     */
    fun clearOldSamplesAndRetry() {
        val empId = _uiState.value.employeeId
        if (empId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, errorMessage = null) }
            employeeRepository.deleteFaceSamples(empId)
            clearanceGatekeeper.reset()
            _uiState.update {
                it.copy(
                    isUploading = false,
                    isBlockedDuplicate = false,
                    currentStep = 1,
                    sampleCount = 0,
                    stabilityProgress = 0f,
                    errorMessage = null,
                    guidanceText = "Đã xóa mẫu cũ. Vui lòng nhìn thẳng camera để chụp mẫu 1/3."
                )
            }
        }
    }
}
