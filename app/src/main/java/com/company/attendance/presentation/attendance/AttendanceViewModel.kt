package com.company.attendance.presentation.attendance

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.logging.logD
import com.company.attendance.core.logging.logE
import com.company.attendance.core.logging.logI
import com.company.attendance.core.result.Result
import com.company.attendance.core.util.toBase64Jpeg
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.ml.FaceEmbedder
import com.company.attendance.domain.ml.LivenessChecker
import com.company.attendance.domain.model.AttendanceRecord
import com.company.attendance.domain.model.FaceAlignmentStatus
import com.company.attendance.domain.model.FaceDetectionData
import com.company.attendance.domain.repository.AttendanceRepository
import com.company.attendance.domain.usecase.RecognizeFaceUseCase
import com.company.attendance.domain.usecase.SubmitAttendanceUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceViewModel @JvmOverloads constructor(
    application: Application,
    private val attendanceRepository: AttendanceRepository = RepositoryModule.attendanceRepository,
    private val recognizeFaceUseCase: RecognizeFaceUseCase = RecognizeFaceUseCase(RepositoryModule.employeeRepository),
    private val submitAttendanceUseCase: SubmitAttendanceUseCase = SubmitAttendanceUseCase(RepositoryModule.attendanceRepository)
) : AndroidViewModel(application) {

    private val faceEmbedder = FaceEmbedder(application.applicationContext)

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AttendanceUiEffect>()

    private val livenessChecker = LivenessChecker()

    private var bitmapProvider: (() -> Bitmap?)? = null

    fun setBitmapProvider(provider: () -> Bitmap?) {
        this.bitmapProvider = provider
    }

    private var clockJob: Job? = null
    private var autoDismissJob: Job? = null
    private var verificationJob: Job? = null

    // Đếm số frame liên tiếp khuôn mặt ổn định
    private var consecutiveAlignedFrames = 0
    private val requiredAlignedFrames = 5 // ~650ms - 800ms giữ ổn định

    // Đếm số frame không có khuôn mặt để xác nhận người vừa chấm đã rời camera
    private var clearanceNoFaceFrames = 0

    // Lưu trackingId và thời điểm chấm công gần nhất để chống chấm lặp liên tục
    private var lastCheckedTrackingId: Int? = null
    private var lastCheckedTimestamp: Long = 0L
    // Multi-Face Tracking & Cooldown
    private val completedTrackingIds = mutableMapOf<Int, Long>()
    private val stableFramesMap = mutableMapOf<Int, Int>()
    private val faceCooldownDurationMs = 10000L
    private val requiredStableFrames = 5 // ~160ms - 200ms giữ ổn định để chụp ảnh sắc nét, chống nhòe chuyển động

    // Best-Shot Selection: Theo dõi độ dịch chuyển tâm mặt (motion displacement)
    // Nếu người dùng đang bước đi quá nhanh (displacement > 18px), reset đếm ổn định để tránh chụp ảnh mờ
    private val lastFaceCenterMap = mutableMapOf<Int, PointF>()

    init {
        startRealtimeClock()
    }

    private fun startRealtimeClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val viLocale = Locale.forLanguageTag("vi-VN")
            val dateFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", viLocale)
            while (isActive) {
                val now = Date()
                val rawDate = dateFormat.format(now)
                val capitalizedDate = rawDate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(viLocale) else it.toString() }
                _uiState.update { current ->
                    current.copy(
                        currentTimeString = timeFormat.format(now),
                        currentDateString = capitalizedDate
                    )
                }
                delay(1000)
            }
        }
    }

    fun onEvent(event: AttendanceUiEvent) {
        when (event) {
            is AttendanceUiEvent.OnFaceFrameAnalyzed -> {
                handleFaceFrame(event.faceData)
            }
            is AttendanceUiEvent.OnCameraPermissionResult -> {
                _uiState.update { current ->
                    current.copy(
                        hasCameraPermission = event.granted,
                        status = if (event.granted) RecognitionStatus.IDLE else RecognitionStatus.PERMISSION_DENIED
                    )
                }
            }
            is AttendanceUiEvent.OnCameraInitialized -> {
                _uiState.update { current ->
                    current.copy(
                        isCameraInitialized = true,
                        cameraErrorMessage = null,
                        status = if (current.status == RecognitionStatus.CAMERA_ERROR) RecognitionStatus.IDLE else current.status
                    )
                }
            }
            is AttendanceUiEvent.OnCameraError -> {
                _uiState.update { current ->
                    current.copy(
                        isCameraInitialized = false,
                        cameraErrorMessage = event.message,
                        status = RecognitionStatus.CAMERA_ERROR
                    )
                }
            }
            is AttendanceUiEvent.OnRetryCamera -> {
                _uiState.update { current ->
                    current.copy(
                        status = RecognitionStatus.IDLE,
                        cameraErrorMessage = null
                    )
                }
            }

            is AttendanceUiEvent.OnDismissResult, is AttendanceUiEvent.OnScanAgain -> {
                resetToIdle()
            }
            is AttendanceUiEvent.OnToggleSettingsDialog -> {
                _uiState.update { it.copy(isSettingsDialogVisible = event.isVisible) }
            }
            is AttendanceUiEvent.OnToggleEmployeeSearch -> {
                val nextScreen = if (event.isVisible) KioskScreen.EMPLOYEE_SEARCH else KioskScreen.ATTENDANCE
                _uiState.update { it.copy(isEmployeeSearchVisible = event.isVisible, currentScreen = nextScreen) }
            }
            is AttendanceUiEvent.OnNavigateToScreen -> {
                _uiState.update {
                    it.copy(
                        currentScreen = event.screen,
                        isSettingsDialogVisible = false,
                        isEmployeeSearchVisible = (event.screen == KioskScreen.EMPLOYEE_SEARCH),
                        selectedEmployee = if (event.screen == KioskScreen.ATTENDANCE) null else it.selectedEmployee,
                        capturedBitmap = if (event.screen == KioskScreen.ATTENDANCE || event.screen == KioskScreen.EMPLOYEE_DETAIL || event.screen == KioskScreen.FACE_CAPTURE) null else it.capturedBitmap
                    )
                }
            }
            is AttendanceUiEvent.OnToggleRealMode -> {
                resetToIdle()
                _uiState.update { it.copy(isRealMode = event.isReal) }
            }
            is AttendanceUiEvent.OnToggleCamera -> {
                _uiState.update { it.copy(isFrontCamera = event.isFront) }
            }

        }
    }

    /**
     * Xử lý frame khuôn mặt theo State Machine:
     * IDLE -> FACE_DETECTED -> ALIGNING -> FACE_STABLE -> VERIFYING -> SUCCESS / FAILURE -> COOLDOWN -> IDLE
     */
        /**
     * Lựa chọn khuôn mặt ưu tiên để gửi lên server:
     * 1. Bỏ qua các khuôn mặt đang trong thời gian Cooldown.
     * 2. Ưu tiên người đang nhìn thẳng vào camera (isLookingStraight).
     * 3. Ưu tiên người có diện tích khuôn mặt lớn nhất (đứng gần camera nhất).
     */
    private fun selectPrimaryCandidate(
        trackedFaces: List<com.company.attendance.domain.model.FaceTrackItem>,
        currentTime: Long
    ): com.company.attendance.domain.model.FaceTrackItem? {
        completedTrackingIds.entries.removeAll { (currentTime - it.value) > faceCooldownDurationMs }

        val availableFaces = trackedFaces.filter { face ->
            val id = face.trackingId ?: return@filter true
            val lastProcessed = completedTrackingIds[id] ?: 0L
            (currentTime - lastProcessed) > faceCooldownDurationMs
        }

        if (availableFaces.isEmpty()) return null

        return availableFaces.filter { it.hasValidFaceFeatures }.maxWithOrNull(
            compareBy<com.company.attendance.domain.model.FaceTrackItem> { it.isLookingStraight }
                .thenBy { it.area }
        )
    }

    /**
     * Xử lý frame khuôn mặt theo State Machine:
     * IDLE -> FACE_DETECTED -> ALIGNING -> FACE_STABLE -> VERIFYING -> SUCCESS / FAILURE -> COOLDOWN -> IDLE
     *
     * Hỗ trợ quét nhiều người đồng thời:
     * - Tự động chọn người ưu tiên tốt nhất (Primary Candidate): Người nhìn thẳng + đứng gần camera nhất.
     * - Khi người ưu tiên đạt độ ổn định (giữ yên ~350ms): Chụp và gửi lên Server.
     * - Sau khi quét xong: Người đó được đưa vào Cooldown để tự động chuyển sang người thứ 2 ngay lập tức!
     */
    private fun handleFaceFrame(faceData: FaceDetectionData?) {
        val currentStatus = _uiState.value.status

        // 0. Nếu đang trong trạng thái hiển thị kết quả (SUCCESS / DUPLICATE / FAILURE):
        if (currentStatus == RecognitionStatus.SUCCESS ||
            currentStatus == RecognitionStatus.DUPLICATE ||
            currentStatus == RecognitionStatus.FAILURE
        ) {
            if (faceData?.trackingId != null && faceData.trackingId != lastCheckedTrackingId) {
                logI("Phát hiện người mới bước vào camera (trackingId: ${faceData.trackingId} != $lastCheckedTrackingId) -> Đổi lượt quét ngay")
                autoDismissJob?.cancel()
                _uiState.update {
                    it.copy(
                        status = RecognitionStatus.IDLE,
                        resultData = null,
                        duplicateMessage = null,
                        isWaitingFaceClearance = false,
                        stabilityProgress = 0f
                    )
                }
            } else if (faceData == null) {
                clearanceNoFaceFrames++
                if (clearanceNoFaceFrames >= 2) {
                    clearanceNoFaceFrames = 0
                    if (_uiState.value.autoDismissRemainingSeconds <= 1) {
                        resetToIdle()
                        return
                    }
                }
                return
            } else {
                clearanceNoFaceFrames = 0
                return
            }
        }

        // Nếu đang trong trạng thái gửi dữ liệu lên AI Server hoặc lỗi thiết bị, không xử lý frame
        if (currentStatus == RecognitionStatus.VERIFYING ||
            currentStatus == RecognitionStatus.CAMERA_ERROR ||
            currentStatus == RecognitionStatus.PERMISSION_DENIED
        ) {
            return
        }

        // Kiểm tra cơ chế Face Clearance
        if (_uiState.value.isWaitingFaceClearance) {
            if (faceData?.trackingId != null && faceData.trackingId != lastCheckedTrackingId) {
                logI("Người mới xuất hiện trong thời gian chờ clearance (trackingId: ${faceData.trackingId}) -> Mở khóa quét ngay")
                clearanceNoFaceFrames = 0
                _uiState.update {
                    it.copy(
                        isWaitingFaceClearance = false,
                        stabilityProgress = 0f
                    )
                }
            } else if (faceData == null) {
                clearanceNoFaceFrames++
                if (clearanceNoFaceFrames >= 2) {
                    clearanceNoFaceFrames = 0
                    _uiState.update {
                        it.copy(
                            isWaitingFaceClearance = false,
                            status = RecognitionStatus.IDLE,
                            activeGuidance = "Vui lòng nhìn thẳng vào camera",
                            confidenceText = "Đang tìm khuôn mặt..."
                        )
                    }
                }
                return
            } else {
                clearanceNoFaceFrames = 0
                _uiState.update {
                    it.copy(
                        status = RecognitionStatus.COOLDOWN,
                        faceData = faceData,
                        activeGuidance = "Bạn vừa hoàn tất chấm công. Vui lòng nhường vị trí cho người tiếp theo.",
                        confidenceText = "Vui lòng nhường vị trí",
                        stabilityProgress = 0f
                    )
                }
                return
            }
        }

        // 1. Không có khuôn mặt trước camera: Ẩn toàn bộ box ô vuông
        if (faceData == null || faceData.allFaces.isEmpty()) {
            consecutiveAlignedFrames = 0
            stableFramesMap.clear()
            lastFaceCenterMap.clear()
            livenessChecker.reset()
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.IDLE,
                    faceData = null,
                    detectedFaces = emptyList(),
                    primaryFaceBox = null,
                    frameWidth = 0,
                    frameHeight = 0,
                    alignmentStatus = FaceAlignmentStatus.NO_FACE,
                    activeGuidance = "Vui lòng đứng trước camera",
                    confidenceText = "Đang tìm khuôn mặt...",
                    stabilityProgress = 0f
                )
            }
            return
        }

        val faces = faceData.allFaces.ifEmpty { listOf(faceData.boundingBox) }
        val now = System.currentTimeMillis()

        // Lấy danh sách toàn bộ khuôn mặt đang được theo dõi
        val trackedFaces = faceData.trackedFaces.ifEmpty {
            listOf(
                com.company.attendance.domain.model.FaceTrackItem(
                    boundingBox = faceData.boundingBox,
                    trackingId = faceData.trackingId,
                    headEulerAngleX = faceData.headEulerAngleX,
                    headEulerAngleY = faceData.headEulerAngleY,
                    headEulerAngleZ = faceData.headEulerAngleZ
                )
            )
        }

        // 2. Chọn người ưu tiên gửi server (chưa bị cooldown, nhìn thẳng, to nhất)
        val primaryCandidate = selectPrimaryCandidate(trackedFaces, now)
        val primaryBox = primaryCandidate?.boundingBox ?: faces.firstOrNull() ?: faceData.boundingBox

        if (primaryCandidate == null) {
            // Toàn bộ các khuôn mặt đều vừa quét xong và đang trong cooldown
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.COOLDOWN,
                    faceData = faceData,
                    detectedFaces = faces,
                    primaryFaceBox = primaryBox,
                    frameWidth = faceData.imageWidth,
                    frameHeight = faceData.imageHeight,
                    alignmentStatus = FaceAlignmentStatus.ALIGNED,
                    activeGuidance = "Đã hoàn tất chấm công. Vui lòng nhường chỗ cho người tiếp theo.",
                    confidenceText = "Đang chờ người tiếp theo...",
                    stabilityProgress = 0f
                )
            }
            return
        }

        // 3. Đánh giá độ ổn định và bắt ảnh nét nhất (Best-Shot Selection)
        val candidateTrackId = primaryCandidate.trackingId ?: primaryCandidate.hashCode()

        // Tính toán độ dịch chuyển tâm khuôn mặt giữa 2 frame liên tiếp (Motion Displacement)
        val currentCenter = PointF(
            primaryBox.exactCenterX(),
            primaryBox.exactCenterY()
        )
        val lastCenter = lastFaceCenterMap[candidateTrackId]
        val displacement = if (lastCenter != null) {
            kotlin.math.hypot(
                (currentCenter.x - lastCenter.x).toDouble(),
                (currentCenter.y - lastCenter.y).toDouble()
            ).toFloat()
        } else {
            0f
        }
        lastFaceCenterMap[candidateTrackId] = currentCenter

        // Điều kiện chụp ảnh sắc nét (Best-Shot):
        // 1. Nhìn thẳng và có đặc trưng khuôn mặt hợp lệ (chống giả lập / ma)
        // 2. Không di chuyển nhanh khi chụp (độ dịch chuyển tâm <= 12px giữa các frame)
        // 3. Đứng đủ gần camera (chiều rộng & cao khuôn mặt >= 110px)
        val isStationary = displacement <= 12f
        val isSufficientSize = primaryBox.width() >= 110 && primaryBox.height() >= 110
        val isLookingStraight = primaryCandidate.isLookingStraight && primaryCandidate.hasValidFaceFeatures

        if (isLookingStraight && isStationary && isSufficientSize) {
            val currentStable = (stableFramesMap[candidateTrackId] ?: 0) + 1
            stableFramesMap[candidateTrackId] = currentStable
            val progress = (currentStable.toFloat() / requiredStableFrames).coerceIn(0f, 1f)

            val statusText = if (faces.size > 1) {
                "Phát hiện ${faces.size} người • Đang quét người chính"
            } else {
                "Đã phát hiện khuôn mặt"
            }

            _uiState.update { current ->
                current.copy(
                    status = if (currentStable >= requiredStableFrames) RecognitionStatus.FACE_STABLE else RecognitionStatus.FACE_DETECTED,
                    faceData = faceData,
                    detectedFaces = faces,
                    primaryFaceBox = primaryBox,
                    frameWidth = faceData.imageWidth,
                    frameHeight = faceData.imageHeight,
                    alignmentStatus = FaceAlignmentStatus.ALIGNED,
                    activeGuidance = if (currentStable >= requiredStableFrames) "Đang chụp và gửi lên AI Server..." else "Giữ yên khuôn mặt để quét...",
                    confidenceText = statusText,
                    stabilityProgress = progress
                )
            }

            // KHI ĐỦ ĐIỀU KIỆN ỔN ĐỊNH: Tự động chụp và gửi lên Server!
            if (currentStable >= requiredStableFrames) {
                stableFramesMap.remove(candidateTrackId)
                lastFaceCenterMap.remove(candidateTrackId)
                triggerVerification(faceData, primaryCandidate)
            }
        } else {
            // Người đó đang quay đầu, di chuyển nhanh hoặc quá xa -> reset đếm ổn định để tránh chụp mờ
            stableFramesMap[candidateTrackId] = 0
            val guidance = when {
                !isSufficientSize -> "Vui lòng tiến lại gần camera hơn"
                !isLookingStraight -> "Vui lòng nhìn thẳng vào camera"
                else -> "Giữ yên khuôn mặt để chụp nét..."
            }
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.ALIGNING,
                    faceData = faceData,
                    detectedFaces = faces,
                    primaryFaceBox = primaryBox,
                    frameWidth = faceData.imageWidth,
                    frameHeight = faceData.imageHeight,
                    alignmentStatus = FaceAlignmentStatus.HEAD_TILTED,
                    activeGuidance = guidance,
                    confidenceText = if (faces.size > 1) "Phát hiện ${faces.size} người" else "Đang căn chỉnh...",
                    stabilityProgress = 0f
                )
            }
        }
    }

    /**
     * Tiến hành bước VERIFYING: Chụp và gửi ảnh khuôn mặt lên AI Server để nhận diện.
     */
    private fun triggerVerification(
        faceData: FaceDetectionData,
        targetFace: com.company.attendance.domain.model.FaceTrackItem? = null
    ) {
        val targetTrackingId = targetFace?.trackingId ?: faceData.trackingId
        verificationJob?.cancel()
        verificationJob = viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.VERIFYING,
                    faceData = faceData,
                    activeGuidance = "Đang nhận diện khuôn mặt qua AI Server...",
                    confidenceText = "Đang đối chiếu...",
                    stabilityProgress = 1f
                )
            }

            // Ghi nhận cooldown cho người này để không bị quét lặp lại liên tục
            targetTrackingId?.let {
                lastCheckedTrackingId = it
                lastCheckedTimestamp = System.currentTimeMillis()
                completedTrackingIds[it] = System.currentTimeMillis()
            }

            if (_uiState.value.isRealMode) {
                val bitmap = bitmapProvider?.invoke()

                if (bitmap != null) {
                    // 1. Xác thực khuôn mặt thực tế trên Bitmap chụp (ngăn chặn triệt để chụp tường trống / phantom face)
                    val exactBox = faceEmbedder.detectFaceBoundingBox(bitmap)
                    if (exactBox == null) {
                        logE("[ATTENDANCE] Bitmap chụp không phát hiện khuôn mặt thực sự -> Bỏ qua tránh phantom check-in")
                        _uiState.update { current ->
                            current.copy(
                                status = RecognitionStatus.IDLE,
                                activeGuidance = "Vui lòng nhìn thẳng vào camera",
                                confidenceText = "Đang tìm khuôn mặt...",
                                stabilityProgress = 0f
                            )
                        }
                        return@launch
                    }

                    // [DEBUG_CAPTURE_TEST] Giữ lại bitmap để hiển thị trực tiếp lên UI test độ nét
                    _uiState.update { it.copy(capturedBitmap = bitmap) }

                    val croppedFace = faceEmbedder.cropFace(bitmap, exactBox, marginRatio = 0.22f) ?: bitmap

                    // 2. Trích xuất vector 512D On-Device bằng TFLite
                    val faceVector = faceEmbedder.extractEmbeddingFromCroppedFace(croppedFace)
                    if (faceVector == null) {
                        logE("[ATTENDANCE] Không thể trích xuất vector khuôn mặt On-Device")
                        if (croppedFace != bitmap) croppedFace.recycle()
                        _uiState.update { current ->
                            current.copy(
                                status = RecognitionStatus.FAILURE,
                                failureReason = "Không thể trích xuất đặc trưng khuôn mặt.",
                                confidenceText = "Lỗi xử lý khuôn mặt",
                                autoDismissRemainingSeconds = 2
                            )
                        }
                        _uiEffect.emit(AttendanceUiEffect.PlayErrorSound)
                        startAutoDismissCountdown()
                        return@launch
                    }

                    // 3. Tạo ảnh thumbnail đại diện nhẹ để lưu vết log
                    val thumbnailBase64 = faceEmbedder.createThumbnailBase64FromCroppedFace(croppedFace, targetSize = 240)
                    if (croppedFace != bitmap) {
                        croppedFace.recycle()
                    }

                    logI("[FACE_CHECKIN] Gửi vector 512D On-Device (dim=${faceVector.size}) lên FaceCheck AI Backend...")
                    val result = attendanceRepository.checkIn(
                        faceVector = faceVector.toList(),
                        imageBase64 = thumbnailBase64
                    )
                    when (result) {
                        is Result.Success -> {
                            val record = result.data
                            val scorePercent = record.matchScore.toInt()
                            val nowTime = record.timeFormatted
                            val attendanceType = record.attendanceType.serverKey
                            val attendanceStatus = record.status.displayName

                            logI("Chấm công thành công [${record.attendanceType.name}]: ${record.employeeName} (${record.employeeId}) - $attendanceStatus")
                            _uiState.update { current ->
                                current.copy(
                                    status = RecognitionStatus.SUCCESS,
                                    confidenceText = "Đã khớp: $scorePercent%",
                                    resultData = AttendanceResultData(
                                        employeeName = record.employeeName,
                                        employeeCode = record.employeeCode,
                                        department = record.department,
                                        checkInTime = nowTime,
                                        matchScore = record.matchScore,
                                        attendanceType = attendanceType,
                                        attendanceStatus = attendanceStatus,
                                        serverMessage = record.serverMessage ?: if (record.attendanceType == com.company.attendance.domain.model.AttendanceType.CHECK_OUT) "Chấm công tan ca thành công" else "Chấm công vào ca thành công",
                                        isDemo = false
                                    ),
                                    autoDismissRemainingSeconds = 2,
                                    isWaitingFaceClearance = true
                                )
                            }
                            _uiEffect.emit(AttendanceUiEffect.PlaySuccessSound)
                            startAutoDismissCountdown()
                        }
                        is Result.Error -> {
                            val error = result.error
                            if (error is com.company.attendance.core.result.AppError.DuplicateAttendance) {
                                logI("Chấm công bị trùng: ${error.message}")
                                _uiState.update { current ->
                                    current.copy(
                                        status = RecognitionStatus.DUPLICATE,
                                        duplicateMessage = error.message,
                                        confidenceText = "Đã ghi nhận",
                                        autoDismissRemainingSeconds = 2,
                                        isWaitingFaceClearance = true
                                    )
                                }
                                _uiEffect.emit(AttendanceUiEffect.PlayWarningSound)
                                startAutoDismissCountdown()
                            } else {
                                logD("Chấm công không thành công: ${error.message}")
                                _uiState.update { current ->
                                    current.copy(
                                        status = RecognitionStatus.FAILURE,
                                        failureReason = error.message,
                                        confidenceText = "Không thành công",
                                        autoDismissRemainingSeconds = 2
                                    )
                                }
                                _uiEffect.emit(AttendanceUiEffect.PlayErrorSound)
                                startAutoDismissCountdown()
                            }
                        }
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(
                            status = RecognitionStatus.FAILURE,
                            failureReason = "Không thể lấy hình ảnh từ camera. Vui lòng căn chỉnh lại.",
                            confidenceText = "Lỗi chụp khung hình",
                            autoDismissRemainingSeconds = 2
                        )
                    }
                    _uiEffect.emit(AttendanceUiEffect.PlayErrorSound)
                    startAutoDismissCountdown()
                }
            } else {
                // Chế độ mô phỏng / offline
                delay(600)
                val result = recognizeFaceUseCase(faceData)
                when (result) {
                    is Result.Success -> {
                        val matchResult = result.data
                        if (matchResult.isMatched && matchResult.matchedEmployee != null) {
                            val emp = matchResult.matchedEmployee
                            val nowTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            val nowDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                            val record = AttendanceRecord(
                                id = System.currentTimeMillis().toString(),
                                employeeId = emp.id,
                                employeeName = emp.name,
                                employeeCode = emp.code,
                                department = emp.department,
                                timestamp = System.currentTimeMillis(),
                                timeFormatted = nowTime,
                                dateFormatted = nowDate,
                                matchScore = matchResult.similarityScore * 100f
                            )
                            submitAttendanceUseCase(record)

                            _uiState.update { current ->
                                current.copy(
                                    status = RecognitionStatus.SUCCESS,
                                    confidenceText = "Đã khớp: ${(matchResult.similarityScore * 100).toInt()}%",
                                    resultData = AttendanceResultData(
                                        employeeName = emp.name,
                                        employeeCode = emp.code,
                                        department = emp.department,
                                        checkInTime = nowTime,
                                        matchScore = matchResult.similarityScore * 100f,
                                        isDemo = true
                                    ),
                                    autoDismissRemainingSeconds = 2
                                )
                            }
                            _uiEffect.emit(AttendanceUiEffect.PlaySuccessSound)
                            startAutoDismissCountdown()
                        } else {
                            _uiState.update { current ->
                                current.copy(
                                    status = RecognitionStatus.FAILURE,
                                    failureReason = matchResult.message,
                                    confidenceText = "Chưa nhận diện danh tính",
                                    autoDismissRemainingSeconds = 2
                                )
                            }
                            _uiEffect.emit(AttendanceUiEffect.PlayErrorSound)
                            startAutoDismissCountdown()
                        }
                    }
                    is Result.Error -> {
                        _uiState.update { current ->
                            current.copy(
                                status = RecognitionStatus.FAILURE,
                                failureReason = result.error.message,
                                confidenceText = "Lỗi nhận diện",
                                autoDismissRemainingSeconds = 2
                            )
                        }
                        _uiEffect.emit(AttendanceUiEffect.PlayErrorSound)
                        startAutoDismissCountdown()
                    }
                }
            }
        }
    }
    private fun startAutoDismissCountdown(seconds: Int = 2) {
        autoDismissJob?.cancel()
        autoDismissJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _uiState.update { it.copy(autoDismissRemainingSeconds = i) }
                delay(1000)
            }
            resetToIdle()
        }
    }

    private fun resetToIdle() {
        autoDismissJob?.cancel()
        verificationJob?.cancel()
        consecutiveAlignedFrames = 0
        stableFramesMap.clear()
        lastFaceCenterMap.clear()
        livenessChecker.reset()
        _uiState.update { current ->
            current.copy(
                status = RecognitionStatus.IDLE,
                faceData = null,
                alignmentStatus = FaceAlignmentStatus.NO_FACE,
                activeGuidance = if (current.isWaitingFaceClearance) "Vui lòng nhường vị trí cho người tiếp theo." else "Vui lòng nhìn thẳng vào camera",
                confidenceText = if (current.isWaitingFaceClearance) "Vui lòng nhường vị trí" else "Đang tìm khuôn mặt...",
                stabilityProgress = 0f,
                resultData = null,
                duplicateMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        autoDismissJob?.cancel()
        verificationJob?.cancel()
        faceEmbedder.close()
    }
}


