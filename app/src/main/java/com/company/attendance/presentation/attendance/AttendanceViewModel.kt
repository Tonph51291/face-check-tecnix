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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

    private val faceEmbedder = com.company.attendance.di.MlModule.getFaceEmbedder(application.applicationContext)

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
    // Phương án 1 (Instant Snap): Chỉ cần 1 frame nhìn thẳng là kích hoạt phân tích & gửi server ngay lập tức
    private val requiredStableFrames = 1

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
                    val oldBitmap = it.capturedBitmap
                    if (oldBitmap != null && !oldBitmap.isRecycled) {
                        try { oldBitmap.recycle() } catch (_: Exception) {}
                    }
                    it.copy(
                        status = RecognitionStatus.IDLE,
                        capturedBitmap = null,
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
                if (current.status == RecognitionStatus.IDLE &&
                    current.faceData == null &&
                    current.detectedFaces.isEmpty() &&
                    current.primaryFaceBox == null &&
                    current.stabilityProgress == 0f
                ) {
                    return@update current
                }
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

        // Điều kiện chụp ảnh siêu tốc (Instant Snap):
        // 1. Nhìn thẳng và có đặc trưng khuôn mặt hợp lệ (chống giả lập / ma)
        // 2. Dung sai di chuyển nới lỏng lên 35px (người dùng vừa bước tới vẫn bắt được ngay, không bị reset oan)
        // 3. Đứng trong khoảng cách hợp lý (chiều rộng & cao khuôn mặt >= 70px, hỗ trợ bắt xa tới 2m)
        // 4. Mắt đang mở
        val isStationary = displacement <= 35f
        val isSufficientSize = primaryBox.width() >= 70 && primaryBox.height() >= 70
        val isEyesOpen = primaryCandidate.areEyesOpen
        val isLookingStraight = primaryCandidate.isLookingStraight && primaryCandidate.hasValidFaceFeatures

        if (isLookingStraight && isEyesOpen && isStationary && isSufficientSize) {
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
            // Người đó đang quay đầu, nhắm mắt, di chuyển nhanh hoặc quá xa -> reset đếm ổn định để tránh chụp mờ
            stableFramesMap[candidateTrackId] = 0
            val guidance = when {
                !isSufficientSize -> "Vui lòng tiến lại gần camera hơn"
                !isLookingStraight -> "Vui lòng nhìn thẳng vào camera"
                !isEyesOpen -> "Vui lòng mở mắt khi quét"
                else -> "Giữ yên khuôn mặt để chụp nét..."
            }
            _uiState.update { current ->
                if (current.status == RecognitionStatus.ALIGNING &&
                    current.activeGuidance == guidance &&
                    current.stabilityProgress == 0f &&
                    current.detectedFaces.size == faces.size
                ) {
                    return@update current
                }
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
                    activeGuidance = "Đang nhận diện khuôn mặt qua",
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
                    val sensorFaceBox = targetFace?.boundingBox ?: faceData.primaryFace?.boundingBox ?: faceData.boundingBox
                    val eulerAngleZ = targetFace?.headEulerAngleZ ?: faceData.headEulerAngleZ

                    // 1. Ánh xạ trực tiếp tọa độ khuôn mặt sang Bitmap (0ms, không cần chạy lại ML Kit lần 2)
                    val exactBox = faceEmbedder.mapBoundingBoxToBitmap(
                        sensorBox = sensorFaceBox,
                        sensorWidth = faceData.imageWidth,
                        sensorHeight = faceData.imageHeight,
                        bitmapWidth = bitmap.width,
                        bitmapHeight = bitmap.height,
                        isFrontCamera = _uiState.value.isFrontCamera
                    )

                    // 2. Cắt khuôn mặt với lề viền an toàn 22%
                    val croppedFace = faceEmbedder.cropFace(bitmap, exactBox, marginRatio = 0.22f) ?: bitmap

                    // 3. GIẢI PHÓNG NGAY LẬP TỨC Bitmap full màn hình 8MB để tiết kiệm RAM triệt để
                    if (croppedFace != bitmap) {
                        bitmap.recycle()
                    }

                    // [DEBUG_CAPTURE_TEST] Giữ lại croppedFace gọn nhẹ (~50KB) để hiển thị lên UI kết quả thay vì giữ cả ảnh 8MB
                    _uiState.update { it.copy(capturedBitmap = croppedFace) }

                    // 4 & 5. Tối ưu song song: Chạy trích xuất vector 512D và tạo Thumbnail đồng thời trên 2 luồng
                    val faceVectorDeferred = async(Dispatchers.Default) {
                        faceEmbedder.extractEmbeddingFromCroppedFace(
                            croppedFace = croppedFace,
                            rotationAngleZ = eulerAngleZ
                        )
                    }
                    val thumbnailDeferred = async(Dispatchers.Default) {
                        faceEmbedder.createThumbnailBase64FromCroppedFace(croppedFace, targetSize = 160, quality = 75)
                    }

                    val faceVector = faceVectorDeferred.await()
                    val thumbnailBase64 = thumbnailDeferred.await()

                    if (faceVector == null) {
                        logE("[ATTENDANCE] Không thể trích xuất vector khuôn mặt On-Device")
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
            val oldBitmap = current.capturedBitmap
            if (oldBitmap != null && !oldBitmap.isRecycled) {
                try { oldBitmap.recycle() } catch (_: Exception) {}
            }
            current.copy(
                status = RecognitionStatus.IDLE,
                faceData = null,
                capturedBitmap = null,
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


