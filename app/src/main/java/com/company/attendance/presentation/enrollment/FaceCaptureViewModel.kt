package com.company.attendance.presentation.enrollment

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.logging.logE
import com.company.attendance.core.logging.logI
import com.company.attendance.core.result.AppError
import com.company.attendance.core.result.Result
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.ml.BitmapQualityAnalyzer
import com.company.attendance.domain.ml.ClearanceDecision
import com.company.attendance.domain.ml.FaceClearanceGatekeeper
import com.company.attendance.domain.ml.FaceEmbedder
import com.company.attendance.domain.ml.enrollment.EnrollmentPoseTracker
import com.company.attendance.domain.ml.enrollment.EnrollmentStep
import com.company.attendance.domain.model.FaceDetectionData
import com.company.attendance.domain.model.FaceQualityMetrics
import com.company.attendance.domain.repository.EmployeeRepository
import com.company.attendance.domain.usecase.EnrollFaceBatchUseCase
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext

/**
 * Hiệu ứng điều hướng và sự kiện một lần cho màn hình FaceCapture
 */
sealed interface FaceCaptureEffect {
    data class NavigateToReview(
        val bitmap: Bitmap?,
        val metrics: FaceQualityMetrics,
        val avatarUrl: String?
    ) : FaceCaptureEffect
    data object NavigateBack : FaceCaptureEffect
}

/**
 * ViewModel điều khiển quy trình Đăng ký khuôn mặt đa góc chuẩn Clean Architecture:
 * - Ủy quyền kiểm tra góc quay và độ ổn định cho [EnrollmentPoseTracker].
 * - Sử dụng phép biến đổi ma trận [FaceEmbedder.mapBoundingBoxToBitmap] (< 0.01ms), triệt tiêu hoàn toàn giật lag ML Kit lần 2.
 * - Phân tích chất lượng ảnh trực tiếp trên ảnh cắt [BitmapQualityAnalyzer.analyzeCroppedFace] chống lệch tọa độ.
 * - Kiểm tra chéo On-Device Cosine Similarity trước khi gọi [EnrollFaceBatchUseCase].
 * - Quản lý an toàn vòng đời Bitmap và khóa camera thông qua [FaceClearanceGatekeeper] khi gặp lỗi 409.
 */
class FaceCaptureViewModel @JvmOverloads constructor(
    application: Application,
    private val employeeRepository: EmployeeRepository = RepositoryModule.employeeRepository,
    private val enrollFaceBatchUseCase: EnrollFaceBatchUseCase = EnrollFaceBatchUseCase(employeeRepository)
) : AndroidViewModel(application) {

    private val faceEmbedder = FaceEmbedder(application.applicationContext)
    private val clearanceGatekeeper = FaceClearanceGatekeeper()
    private val poseTracker = EnrollmentPoseTracker()

    private val _uiState = MutableStateFlow(FaceCaptureUiState())
    val uiState: StateFlow<FaceCaptureUiState> = _uiState.asStateFlow()

    private val _effect = Channel<FaceCaptureEffect>(Channel.BUFFERED)
    val effect: Flow<FaceCaptureEffect> = _effect.receiveAsFlow()

    private var bitmapProvider: (() -> Bitmap?)? = null
    private var cooldownJob: Job? = null
    private var isCoolingDown = false
    private var isCapturingStep = false
    private var latestFaceData: FaceDetectionData? = null

    // Quản lý bộ nhớ RAM tạm thời cho Batch Enrollment (chỉ gửi lên Server khi đủ 3 góc)
    private val capturedVectors = ArrayList<List<Float>>(3)
    private var firstStepThumbnailBase64: String? = null
    private var firstStepBitmap: Bitmap? = null
    private var firstStepQualityMetrics: FaceQualityMetrics? = null

    /**
     * Khởi tạo hoặc đặt lại trạng thái đăng ký
     */
    fun resetEnrollment(employeeId: String) {
        if (employeeId.isBlank()) return
        cooldownJob?.cancel()
        isCoolingDown = false
        isCapturingStep = false
        clearanceGatekeeper.reset()
        poseTracker.reset()
        capturedVectors.clear()
        firstStepThumbnailBase64 = null
        recycleFirstStepBitmap()
        firstStepQualityMetrics = null
        employeeRepository.clearDetailCache(employeeId)

        _uiState.update {
            it.copy(
                employeeId = employeeId,
                currentStep = 1,
                sampleCount = 0,
                samplesSuccess = listOf(false, false, false),
                stabilityProgress = 0f,
                errorMessage = null,
                isConflictDialogVisible = false,
                conflictErrorMessage = null,
                isMismatchDialogVisible = false,
                mismatchErrorMessage = null,
                lastCapturedBitmap = null,
                isUploading = false,
                uploadStatusMessage = null,
                isFaceAligned = false,
                isBlockedDuplicate = false,
                avatarUrl = null,
                guidanceState = TurnGuidanceState.WAITING_TURN,
                guidanceText = "Nhìn thẳng vào giữa camera",
                distanceText = "Khoảng cách hợp lệ (65 cm) • Độ sáng: Tốt",
                showLeftArrow = false,
                showRightArrow = false
            )
        }
        logI("Đặt lại phiên đăng ký khuôn mặt đa góc cho nhân viên: $employeeId")
    }

    fun initEmployee(employeeId: String, forceReset: Boolean = false) {
        if (employeeId.isNotBlank()) {
            if (forceReset || _uiState.value.employeeId != employeeId) {
                resetEnrollment(employeeId)
            }
        }
    }

    fun onCancelClicked() {
        val empId = _uiState.value.employeeId
        isCapturingStep = false
        capturedVectors.clear()
        firstStepThumbnailBase64 = null
        recycleFirstStepBitmap()
        firstStepQualityMetrics = null
        if (empId.isNotBlank()) {
            employeeRepository.clearDetailCache(empId)
            resetEnrollment(empId)
        }
        viewModelScope.launch {
            _effect.send(FaceCaptureEffect.NavigateBack)
        }
    }

    fun setBitmapProvider(provider: () -> Bitmap?) {
        this.bitmapProvider = provider
    }

    /**
     * Xử lý từng frame từ CameraX / ML Kit theo thời gian thực:
     */
    fun onFaceFrame(faceData: FaceDetectionData?) {
        this.latestFaceData = faceData
        val state = _uiState.value
        if (state.isUploading || isCoolingDown || isCapturingStep || state.isConflictDialogVisible || state.isMismatchDialogVisible) {
            return
        }

        // 1. Đánh giá cổng kiểm soát FaceClearanceGatekeeper
        val clearanceDecision = clearanceGatekeeper.evaluate(faceData)
        when (clearanceDecision) {
            is ClearanceDecision.Blocked -> {
                _uiState.update {
                    it.copy(
                        isFaceAligned = false,
                        stabilityProgress = 0f,
                        isBlockedDuplicate = true,
                        guidanceState = TurnGuidanceState.POSITION_INVALID,
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
                        guidanceState = TurnGuidanceState.WAITING_TURN,
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

        // 2. Chuyển cho EnrollmentPoseTracker đánh giá góc mặt & tiến trình ổn định
        val eval = poseTracker.evaluatePose(faceData)
        _uiState.update {
            it.copy(
                isFaceAligned = eval.isAligned,
                stabilityProgress = eval.progress,
                guidanceState = eval.guidanceState,
                guidanceText = eval.guidanceText,
                distanceText = eval.distanceText,
                showLeftArrow = eval.showLeftArrow,
                showRightArrow = eval.showRightArrow
            )
        }

        // 3. Đạt 100% độ ổn định -> Chụp ảnh và trích xuất vector ngay lập tức!
        if (eval.isStepCompleted && faceData != null) {
            triggerStepCapture(faceData)
        }
    }

    /**
     * Cắt ảnh khuôn mặt và trích xuất Vector 512D trên nền Dispatchers.Default:
     * - Dùng mapBoundingBoxToBitmap: tốc độ tức thì (< 0.01ms), hoàn toàn không gọi lại ML Kit lần 2.
     * - Phân tích chất lượng ảnh trực tiếp trên ảnh khuôn mặt đã crop (CroppedFace).
     */
    private fun triggerStepCapture(faceData: FaceDetectionData) {
        val state = _uiState.value
        if (state.isUploading || isCoolingDown || isCapturingStep) return

        val fullBitmap = bitmapProvider?.invoke()
        if (fullBitmap == null) {
            logE("Không thể chụp bitmap từ camera")
            return
        }

        isCapturingStep = true
        _uiState.update { it.copy(stabilityProgress = 0f) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Ánh xạ tọa độ Sensor sang Bitmap tức thì qua phép biến đổi ma trận
                val faceBox = faceEmbedder.mapBoundingBoxToBitmap(
                    sensorBox = faceData.boundingBox,
                    sensorWidth = faceData.imageWidth,
                    sensorHeight = faceData.imageHeight,
                    bitmapWidth = fullBitmap.width,
                    bitmapHeight = fullBitmap.height,
                    isFrontCamera = false
                )

                // 2. Cắt khuôn mặt chuẩn xác làm mẫu trích xuất & avatar
                val croppedFace = faceEmbedder.cropFace(fullBitmap, faceBox, marginRatio = 0.22f) ?: fullBitmap

                // 3. Trích xuất Vector 512D bằng TFLite FaceNet
                val faceVector = faceEmbedder.extractEmbeddingFromCroppedFace(croppedFace)
                if (faceVector == null) {
                    if (croppedFace != fullBitmap && !fullBitmap.isRecycled) {
                        try { fullBitmap.recycle() } catch (_: Exception) {}
                    }
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadStatusMessage = null,
                            errorMessage = "Không thể trích xuất vector khuôn mặt. Vui lòng thử lại."
                        )
                    }
                    return@launch
                }

                // 4. Đo lường chất lượng ảnh trực tiếp trên CroppedFace (loại trừ lệch tọa độ)
                val qualityMetrics = BitmapQualityAnalyzer.analyzeCroppedFace(croppedFace, faceData)

                // Giải phóng ngay Bitmap full màn hình (8MB) sau khi đã cắt xong
                if (croppedFace != fullBitmap && !fullBitmap.isRecycled) {
                    try { fullBitmap.recycle() } catch (_: Exception) {}
                }

                when (poseTracker.currentStep) {
                    EnrollmentStep.FRONTAL -> {
                        // Bước 1: Lưu Vector 1 + tạo thumbnail làm Avatar từ góc chính diện
                        val thumbnailBase64 = faceEmbedder.createThumbnailBase64FromCroppedFace(croppedFace, targetSize = 320)
                        firstStepThumbnailBase64 = thumbnailBase64
                        recycleFirstStepBitmap()
                        firstStepBitmap = croppedFace
                        firstStepQualityMetrics = qualityMetrics

                        capturedVectors.clear()
                        capturedVectors.add(faceVector.toList())
                        poseTracker.moveToNextStep()

                        logI("Đã lưu Mẫu 1 (Chính diện) vào RAM (dim=${faceVector.size}, hasAvatar=${thumbnailBase64 != null})")

                        _uiState.update {
                            it.copy(
                                currentStep = 2,
                                sampleCount = 1,
                                samplesSuccess = listOf(true, false, false),
                                stabilityProgress = 0f,
                                isFaceAligned = false,
                                guidanceState = TurnGuidanceState.WAITING_TURN,
                                guidanceText = "👉 Tuyệt vời! Hãy xoay nhẹ mặt sang bên TRÁI (~15°)",
                                showLeftArrow = true,
                                showRightArrow = false,
                                lastCapturedBitmap = croppedFace,
                                lastQualityMetrics = qualityMetrics
                            )
                        }
                        startCooldown(600)
                    }

                    EnrollmentStep.TURN_LEFT -> {
                        // Bước 2: Lưu Vector 2 (Nghiêng trái)
                        if (capturedVectors.size >= 2) {
                            capturedVectors[1] = faceVector.toList()
                        } else {
                            capturedVectors.add(faceVector.toList())
                        }
                        poseTracker.moveToNextStep()

                        logI("Đã lưu Mẫu 2 (Nghiêng trái) vào RAM. Tổng mẫu: ${capturedVectors.size}/3")

                        _uiState.update {
                            it.copy(
                                currentStep = 3,
                                sampleCount = 2,
                                samplesSuccess = listOf(true, true, false),
                                stabilityProgress = 0f,
                                isFaceAligned = false,
                                guidanceState = TurnGuidanceState.WAITING_TURN,
                                guidanceText = "👈 Rất tốt! Hãy xoay nhẹ mặt sang bên PHẢI (~15°)",
                                showLeftArrow = false,
                                showRightArrow = true
                            )
                        }
                        startCooldown(600)
                    }

                    EnrollmentStep.TURN_RIGHT -> {
                        // Bước 3: Lưu Vector 3 (Nghiêng phải)
                        if (capturedVectors.size >= 3) {
                            capturedVectors[2] = faceVector.toList()
                        } else {
                            capturedVectors.add(faceVector.toList())
                        }

                        val finalBatchVectors = capturedVectors.take(3)
                        logI("Đã thu thập đủ ${finalBatchVectors.size} mẫu trong RAM. Kiểm tra tương đồng On-Device...")

                        // Kiểm tra chéo On-Device: Góc Trái & Góc Phải phải tương đồng với Góc Thẳng (ngưỡng 0.50)
                        if (finalBatchVectors.size == 3) {
                            val simLeft = faceEmbedder.calculateCosineSimilarity(
                                finalBatchVectors[0].toFloatArray(),
                                finalBatchVectors[1].toFloatArray()
                            )
                            val simRight = faceEmbedder.calculateCosineSimilarity(
                                finalBatchVectors[0].toFloatArray(),
                                finalBatchVectors[2].toFloatArray()
                            )
                            logI("Tương đồng On-Device: Thẳng vs Trái = ${"%.4f".format(simLeft)}, Thẳng vs Phải = ${"%.4f".format(simRight)}")

                            if (simLeft < 0.50f || simRight < 0.50f) {
                                logE("Cảnh báo: Mẫu nghiêng không khớp mẫu thẳng (simLeft=$simLeft, simRight=$simRight)")
                                _uiState.update {
                                    it.copy(
                                        stabilityProgress = 0f,
                                        errorMessage = "Khuôn mặt quay chưa khớp hoặc bị bóng lóa. Vui lòng thử lại Bước 3."
                                    )
                                }
                                startCooldown(1000)
                                return@launch
                            }
                        }

                        _uiState.update {
                            it.copy(
                                currentStep = 3,
                                sampleCount = 3,
                                samplesSuccess = listOf(true, true, true),
                                stabilityProgress = 1f,
                                isFaceAligned = true,
                                isUploading = true,
                                uploadStatusMessage = "Đang đồng bộ 3 vector khuôn mặt lên máy chủ...",
                                showLeftArrow = false,
                                showRightArrow = false
                            )
                        }

                        executeBatchEnrollment(
                            employeeId = state.employeeId,
                            vectorsToSend = finalBatchVectors,
                            fallbackBitmap = firstStepBitmap ?: croppedFace,
                            fallbackMetrics = firstStepQualityMetrics ?: qualityMetrics
                        )
                    }
                }
            } finally {
                isCapturingStep = false
            }
        }
    }

    private suspend fun executeBatchEnrollment(
        employeeId: String,
        vectorsToSend: List<List<Float>>,
        fallbackBitmap: Bitmap,
        fallbackMetrics: FaceQualityMetrics
    ) {
        val result = enrollFaceBatchUseCase(
            employeeId = employeeId,
            vectors = vectorsToSend,
            faceImageBase64 = firstStepThumbnailBase64
        )

        when (result) {
            is Result.Success -> {
                val enrollData = result.data
                val updatedAvatarUrl = enrollData.avatarUrl ?: _uiState.value.avatarUrl
                val finalBitmap = firstStepBitmap ?: fallbackBitmap
                val finalMetrics = firstStepQualityMetrics ?: fallbackMetrics

                logI("Đăng ký BATCH thành công! Avatar: $updatedAvatarUrl, Nhân viên: ${enrollData.userName ?: employeeId}")
                capturedVectors.clear()

                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadStatusMessage = null,
                        avatarUrl = updatedAvatarUrl,
                        lastCapturedBitmap = finalBitmap,
                        lastQualityMetrics = finalMetrics,
                        guidanceState = TurnGuidanceState.ALIGNED_HOLDING,
                        guidanceText = "Đã hoàn tất đăng ký đủ 3 mẫu khuôn mặt!"
                    )
                }

                // Gửi ảnh Mẫu 1 (chính diện) sang màn hình FaceReviewScreen
                _effect.send(FaceCaptureEffect.NavigateToReview(finalBitmap, finalMetrics, updatedAvatarUrl))
            }

            is Result.Error -> {
                val error = result.error
                logE("Lỗi đăng ký BATCH: ${error.message}")

                when (error) {
                    is AppError.FaceConflict -> {
                        // Lỗi 409: Trùng khuôn mặt với nhân viên khác trong hệ thống
                        // Kích hoạt ngay FaceClearanceGatekeeper để chặn camera người này!
                        val trackingId = latestFaceData?.trackingId
                        clearanceGatekeeper.blockFace(trackingId, error.message)

                        capturedVectors.clear()
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadStatusMessage = null,
                                isConflictDialogVisible = true,
                                conflictErrorMessage = error.message
                            )
                        }
                    }
                    is AppError.FaceSampleMismatch -> {
                        // Lỗi 400: Không khớp giữa các góc mặt
                        if (capturedVectors.size >= 3) {
                            capturedVectors.removeAt(2)
                        }
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadStatusMessage = null,
                                isMismatchDialogVisible = true,
                                mismatchErrorMessage = error.message
                            )
                        }
                    }
                    else -> {
                        // Lỗi mạng hoặc server: Cho phép người dùng thử gửi lại
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadStatusMessage = null,
                                errorMessage = error.message
                            )
                        }
                        startCooldown(1500)
                    }
                }
            }
        }
    }

    fun retryBatchUpload() {
        val state = _uiState.value
        val validVectors = capturedVectors.take(3)
        if (validVectors.size == 3 && state.employeeId.isNotBlank()) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isUploading = true,
                        uploadStatusMessage = "Đang thử gửi lại 3 vector khuôn mặt...",
                        errorMessage = null,
                        isMismatchDialogVisible = false,
                        isConflictDialogVisible = false
                    )
                }
                executeBatchEnrollment(
                    employeeId = state.employeeId,
                    vectorsToSend = validVectors,
                    fallbackBitmap = firstStepBitmap ?: _uiState.value.lastCapturedBitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                    fallbackMetrics = firstStepQualityMetrics ?: _uiState.value.lastQualityMetrics ?: FaceQualityMetrics()
                )
            }
        } else {
            clearOldSamplesAndRetry()
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

    fun dismissConflictDialog() {
        _uiState.update {
            it.copy(
                isConflictDialogVisible = false,
                conflictErrorMessage = null
            )
        }
    }

    fun dismissMismatchDialog() {
        isCapturingStep = false
        _uiState.update {
            it.copy(
                isMismatchDialogVisible = false,
                mismatchErrorMessage = null,
                currentStep = 3,
                sampleCount = 2,
                stabilityProgress = 0f,
                guidanceState = TurnGuidanceState.WAITING_TURN,
                guidanceText = "👈 Vui lòng xoay nhẹ mặt sang bên PHẢI (~15°)",
                showLeftArrow = false,
                showRightArrow = true
            )
        }
        startCooldown(1000)
    }

    fun dismissError() {
        clearanceGatekeeper.reset()
        _uiState.update {
            it.copy(
                errorMessage = null,
                isBlockedDuplicate = false,
                guidanceText = "Nhìn thẳng vào giữa camera"
            )
        }
    }

    /**
     * Xóa mẫu cũ và bắt đầu lại từ Bước 1
     */
    fun clearOldSamplesAndRetry() {
        viewModelScope.launch {
            val empId = _uiState.value.employeeId
            isCapturingStep = false
            capturedVectors.clear()
            firstStepThumbnailBase64 = null
            recycleFirstStepBitmap()
            firstStepQualityMetrics = null
            if (empId.isNotBlank()) {
                employeeRepository.deleteFaceSamples(empId)
                employeeRepository.clearDetailCache(empId)
                resetEnrollment(empId)
            }
        }
    }

    private fun recycleFirstStepBitmap() {
        firstStepBitmap?.let {
            if (!it.isRecycled) {
                try { it.recycle() } catch (_: Exception) {}
            }
        }
        firstStepBitmap = null
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
        recycleFirstStepBitmap()
        faceEmbedder.close()
    }
}
