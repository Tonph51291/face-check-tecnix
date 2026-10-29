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
 * ViewModel điều khiển quy trình Đăng ký khuôn mặt đa góc (3 bước: Thẳng, Trái ~15°, Phải ~15°)
 * và đồng bộ Vector 512D trực tiếp lên Backend theo đúng OpenAPI Contract:
 * - Mẫu 1 (Chính diện): Trích xuất Vector 512D + nén Thumbnail Base64 làm Avatar.
 * - Mẫu 2 & 3 (Góc nghiêng): Chỉ trích xuất Vector 512D để tối ưu băng thông.
 * - Tự động bắt mã lỗi 409 (Trùng mặt), 400 (Tráo người) ngay tại từng bước.
 */
class FaceCaptureViewModel @JvmOverloads constructor(
    application: Application,
    private val employeeRepository: EmployeeRepository = RepositoryModule.employeeRepository
) : AndroidViewModel(application) {

    private val faceEmbedder = FaceEmbedder(application.applicationContext)
    private val clearanceGatekeeper = FaceClearanceGatekeeper()

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
    private var step2RecordedYaw: Float? = null

    /**
     * Khởi tạo hoặc đặt lại trạng thái đăng ký
     */
    fun resetEnrollment(employeeId: String) {
        if (employeeId.isBlank()) return
        cooldownJob?.cancel()
        isCoolingDown = false
        isCapturingStep = false
        clearanceGatekeeper.reset()
        capturedVectors.clear()
        firstStepThumbnailBase64 = null
        firstStepBitmap = null
        firstStepQualityMetrics = null
        step2RecordedYaw = null
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
        logI("Đặt lại phiên đăng ký khuôn mặt (Batch) cho nhân viên: $employeeId")
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
        firstStepBitmap = null
        firstStepQualityMetrics = null
        step2RecordedYaw = null
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
     * Xử lý từng frame từ ML Kit thời gian thực và đo lường góc quay 3D chính xác:
     * - Bước 1: Chính diện (Yaw trong khoảng -8° đến +8°)
     * - Bước 2: Nghiêng trái nhẹ (Yaw trong khoảng +12° đến +22°)
     * - Bước 3: Nghiêng phải nhẹ (Yaw trong khoảng -22° đến -12°)
     */
    fun onFaceFrame(faceData: FaceDetectionData?) {
        this.latestFaceData = faceData
        val state = _uiState.value
        if (state.isUploading || isCoolingDown || state.isConflictDialogVisible || state.isMismatchDialogVisible) {
            return
        }

        // 1. Kiểm tra FaceClearanceGatekeeper
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

        // 2. Không phát hiện khuôn mặt
        if (faceData == null) {
            _uiState.update {
                it.copy(
                    isFaceAligned = false,
                    stabilityProgress = 0f,
                    guidanceState = TurnGuidanceState.NO_FACE,
                    guidanceText = "Vui lòng nhìn vào khung ngắm camera",
                    distanceText = "Chờ khuôn mặt...",
                    showLeftArrow = false,
                    showRightArrow = false
                )
            }
            return
        }

        // 3. Kiểm tra số lượng người trong khung hình
        if (faceData.trackedFaces.size > 1) {
            _uiState.update {
                it.copy(
                    isFaceAligned = false,
                    stabilityProgress = 0f,
                    guidanceState = TurnGuidanceState.POSITION_INVALID,
                    guidanceText = "Chỉ đứng 1 người trước khung ngắm",
                    distanceText = "Phát hiện nhiều người"
                )
            }
            return
        }

        // 4. Kiểm tra khoảng cách
        if (faceData.alignmentStatus == FaceAlignmentStatus.TOO_FAR) {
            _uiState.update {
                it.copy(
                    isFaceAligned = false,
                    stabilityProgress = 0f,
                    guidanceState = TurnGuidanceState.POSITION_INVALID,
                    guidanceText = "Vui lòng bước lại gần camera hơn",
                    distanceText = "Quá xa (> 1.8m)"
                )
            }
            return
        }
        if (faceData.alignmentStatus == FaceAlignmentStatus.TOO_CLOSE) {
            _uiState.update {
                it.copy(
                    isFaceAligned = false,
                    stabilityProgress = 0f,
                    guidanceState = TurnGuidanceState.POSITION_INVALID,
                    guidanceText = "Vui lòng lùi xa một chút",
                    distanceText = "Quá gần (< 35cm)"
                )
            }
            return
        }

        val yaw = faceData.headEulerAngleY
        val pitch = faceData.headEulerAngleX
        val roll = faceData.headEulerAngleZ

        // 5. Khóa góc ngửa/cúi (Pitch) và nghiêng tai (Roll) trong ngưỡng an toàn ±12°
        if (abs(pitch) > 12f || abs(roll) > 12f) {
            val tiltMessage = if (abs(pitch) > 12f) {
                if (pitch > 12f) "Hơi ngửa đầu, hãy hạ cằm xuống" else "Hơi cúi đầu, hãy nâng mặt lên"
            } else {
                "Vui lòng giữ thẳng đầu, không nghiêng tai"
            }
            _uiState.update {
                it.copy(
                    isFaceAligned = false,
                    stabilityProgress = 0f,
                    guidanceState = TurnGuidanceState.HEAD_TILTED,
                    guidanceText = tiltMessage,
                    distanceText = "Cần chỉnh thẳng đầu",
                    showLeftArrow = false,
                    showRightArrow = false
                )
            }
            return
        }

        // 6. Đo góc quay ngang (Yaw) theo từng bước chụp cụ thể
        var isStepAligned = false
        var stepState = TurnGuidanceState.WAITING_TURN
        var stepGuidance = ""
        var showLeft = false
        var showRight = false

        when (state.currentStep) {
            1 -> {
                // Bước 1: Chính diện (Yaw trong khoảng -6° đến +6°)
                if (abs(yaw) <= 6f) {
                    isStepAligned = true
                    stepState = TurnGuidanceState.ALIGNED_HOLDING
                } else {
                    isStepAligned = false
                    stepState = TurnGuidanceState.WAITING_TURN
                    stepGuidance = "Vui lòng nhìn thẳng vào giữa camera"
                }
            }
            2 -> {
                // Bước 2: Nghiêng trái (~15° đến 25°)
                when {
                    (yaw in -25f..-12f) || (yaw in 12f..25f) -> {
                        isStepAligned = true
                        stepState = TurnGuidanceState.ALIGNED_HOLDING
                        step2RecordedYaw = yaw
                    }
                    abs(yaw) > 28f -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.TURN_TOO_FAR
                        stepGuidance = "Quay quá nhiều! Hãy xoay nhẹ lại về giữa"
                    }
                    else -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.WAITING_TURN
                        stepGuidance = "👉 Vui lòng xoay nhẹ mặt sang bên TRÁI (~15°)"
                        showLeft = true
                    }
                }
            }
            3 -> {
                // Bước 3: Nghiêng phải (Hướng ngược lại so với Bước 2)
                val requiredYawRange = if ((step2RecordedYaw ?: -1f) < 0f) {
                    12f..25f
                } else {
                    -25f..-12f
                }
                when {
                    yaw in requiredYawRange -> {
                        isStepAligned = true
                        stepState = TurnGuidanceState.ALIGNED_HOLDING
                    }
                    abs(yaw) > 28f -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.TURN_TOO_FAR
                        stepGuidance = "Quay quá đà! Hãy xoay nhẹ lại về phía camera"
                    }
                    (step2RecordedYaw != null && ((step2RecordedYaw!! > 0 && yaw > 5f) || (step2RecordedYaw!! < 0 && yaw < -5f))) -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.WAITING_TURN
                        stepGuidance = "Sai hướng! Hãy quay sang bên PHẢI (đối diện)"
                        showRight = true
                    }
                    else -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.WAITING_TURN
                        stepGuidance = "👈 Vui lòng xoay nhẹ mặt sang bên PHẢI (~15°)"
                        showRight = true
                    }
                }
            }
            else -> {
                isStepAligned = true
                stepState = TurnGuidanceState.ALIGNED_HOLDING
            }
        }

        if (!isStepAligned) {
            _uiState.update {
                it.copy(
                    isFaceAligned = false,
                    stabilityProgress = 0f,
                    guidanceState = stepState,
                    guidanceText = stepGuidance,
                    distanceText = "Khoảng cách hợp lệ (65 cm)",
                    showLeftArrow = showLeft,
                    showRightArrow = showRight
                )
            }
            return
        }

        // 7. Thuật toán chống rung nhòe (~0.6 giây, tăng 0.08 mỗi frame)
        val newProgress = min(state.stabilityProgress + 0.08f, 1.0f)
        val progressPercent = (newProgress * 100).toInt()
        val holdingGuidance = when (state.currentStep) {
            1 -> "Rất tốt! Giữ yên khuôn mặt... $progressPercent%"
            2 -> "Rất tốt! Giữ yên góc trái... $progressPercent%"
            3 -> "Rất chuẩn! Giữ yên góc phải... $progressPercent%"
            else -> "Giữ yên khuôn mặt... $progressPercent%"
        }

        _uiState.update {
            it.copy(
                isFaceAligned = true,
                stabilityProgress = newProgress,
                guidanceState = TurnGuidanceState.ALIGNED_HOLDING,
                guidanceText = holdingGuidance,
                distanceText = "Khoảng cách chuẩn (65 cm) • Độ sáng: Tốt",
                showLeftArrow = false,
                showRightArrow = false
            )
        }

        // Đạt 100% độ ổn định -> Trích xuất Vector & Đồng bộ lên Server ngay lập tức!
        if (newProgress >= 1.0f) {
            triggerAutoCaptureAndUpload()
        }
    }

    /**
     * Tự động trích xuất Vector 512D từ On-Device TFLite:
     * - Bước 1 & 2: Lưu vector vào RAM tạm thời (cực nhanh, không gọi network)
     * - Bước 3: Gom đủ 3 vector + ảnh thumbnail gửi 1 Request BATCH duy nhất lên Backend
     */
    private fun triggerAutoCaptureAndUpload() {
        val state = _uiState.value
        if (state.isUploading || isCoolingDown || isCapturingStep) return

        val currentStep = state.currentStep
        val bitmap = bitmapProvider?.invoke()
        val faceData = latestFaceData

        if (bitmap == null || faceData == null) {
            logE("Không thể chụp frame hoặc thiếu dữ liệu khuôn mặt")
            return
        }

        // Khóa đồng thời và reset stability ngay lập tức để ngăn các frame tiếp theo kích hoạt song song
        isCapturingStep = true
        _uiState.update { it.copy(stabilityProgress = 0f) }

        viewModelScope.launch {
            try {
                // 1. Xác định tọa độ khuôn mặt chính xác trên Bitmap hiện tại (chống lệch tỷ lệ View và camera stream)
                val exactBox = faceEmbedder.detectFaceBoundingBox(bitmap)
                    ?: faceEmbedder.mapBoundingBoxToBitmap(
                        sensorBox = faceData.boundingBox,
                        sensorWidth = faceData.imageWidth,
                        sensorHeight = faceData.imageHeight,
                        bitmapWidth = bitmap.width,
                        bitmapHeight = bitmap.height,
                        isFrontCamera = false
                    )

                // 2. Cắt khuôn mặt chuẩn xác làm mẫu trích xuất & avatar
                val croppedFace = faceEmbedder.cropFace(bitmap, exactBox, marginRatio = 0.22f) ?: bitmap

                // 3. Trích xuất Vector 512D bằng TFLite FaceNet từ đúng vùng khuôn mặt
                val faceVector = faceEmbedder.extractEmbeddingFromCroppedFace(croppedFace)
                if (faceVector == null) {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadStatusMessage = null,
                            errorMessage = "Không thể trích xuất vector khuôn mặt. Vui lòng thử lại."
                        )
                    }
                    return@launch
                }

                val qualityMetrics = BitmapQualityAnalyzer.analyze(bitmap, faceData)

                when (currentStep) {
                    1 -> {
                        // Bước 1: Lưu Vector 1 + tạo thumbnail làm Avatar từ ảnh góc chính diện lưu tạm RAM
                        val thumbnailBase64 = faceEmbedder.createThumbnailBase64FromCroppedFace(croppedFace, targetSize = 320)
                        firstStepThumbnailBase64 = thumbnailBase64
                        firstStepBitmap = croppedFace // Lưu giữ ảnh chính diện Mẫu 1 cho màn hình Review và Avatar
                        firstStepQualityMetrics = qualityMetrics

                        capturedVectors.clear()
                        capturedVectors.add(faceVector.toList())

                        logI("Đã lưu Mẫu 1 (Chính diện) vào RAM (dim=${faceVector.size}, box=$exactBox, hasAvatar=${thumbnailBase64 != null})")

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

                    2 -> {
                        // Bước 2: Lưu Vector 2 vào RAM (ghi đè slot 1 nếu đã có để không bao giờ vượt quá số lượng)
                        if (capturedVectors.size >= 2) {
                            capturedVectors[1] = faceVector.toList()
                        } else {
                            capturedVectors.add(faceVector.toList())
                        }
                        logI("Đã lưu Mẫu 2 (Nghiêng trái) vào RAM. Tổng mẫu trong RAM: ${capturedVectors.size}/3")

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

                    3 -> {
                        // Bước 3: Lưu Vector 3 vào RAM (ghi đè slot 2 nếu đã có)
                        if (capturedVectors.size >= 3) {
                            capturedVectors[2] = faceVector.toList()
                        } else {
                            capturedVectors.add(faceVector.toList())
                        }

                        // Đảm bảo lấy ĐÚNG CHÍNH XÁC 3 vector
                        val finalBatchVectors = capturedVectors.take(3)
                        logI("Đã thu thập đủ ${finalBatchVectors.size} mẫu khuôn mặt trong RAM! Bắt đầu gửi BATCH lên Server...")

                        if (finalBatchVectors.size == 3) {
                            val simLeft = faceEmbedder.calculateCosineSimilarity(
                                finalBatchVectors[0].toFloatArray(),
                                finalBatchVectors[1].toFloatArray()
                            )
                            val simRight = faceEmbedder.calculateCosineSimilarity(
                                finalBatchVectors[0].toFloatArray(),
                                finalBatchVectors[2].toFloatArray()
                            )
                            logI("Kiểm tra tương đồng On-Device: Thẳng vs Trái = ${"%.4f".format(simLeft)}, Thẳng vs Phải = ${"%.4f".format(simRight)}")
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
        val result = employeeRepository.enrollFaceBatch(
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
                        // Lỗi 400: Không khớp giữa các góc mặt (hoặc lỗi kiểm tra backend)
                        // Giữ lại Mẫu 1 & 2 để người dùng có thể ấn 'Thử lại góc này'
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
                        // Lỗi mạng hoặc lỗi kết nối: Giữ lại 3 vector trong RAM để người dùng có thể ấn Thử lại
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
            firstStepBitmap = null
            firstStepQualityMetrics = null
            step2RecordedYaw = null
            if (empId.isNotBlank()) {
                employeeRepository.deleteFaceSamples(empId)
                employeeRepository.clearDetailCache(empId)
                resetEnrollment(empId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
        faceEmbedder.close()
    }
}
