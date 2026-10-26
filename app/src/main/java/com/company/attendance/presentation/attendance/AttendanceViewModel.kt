package com.company.attendance.presentation.attendance

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.logging.logD
import com.company.attendance.core.logging.logI
import com.company.attendance.core.result.Result
import com.company.attendance.core.util.toBase64Jpeg
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.ml.LivenessChecker
import com.company.attendance.domain.model.AttendanceRecord
import com.company.attendance.domain.model.FaceAlignmentStatus
import com.company.attendance.domain.model.FaceDetectionData
import com.company.attendance.domain.repository.AttendanceRepository
import com.company.attendance.domain.repository.EmployeeRepository
import com.company.attendance.domain.usecase.EnrollFaceUseCase
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

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository = RepositoryModule.attendanceRepository,
    private val recognizeFaceUseCase: RecognizeFaceUseCase = RecognizeFaceUseCase(RepositoryModule.employeeRepository),
    private val submitAttendanceUseCase: SubmitAttendanceUseCase = SubmitAttendanceUseCase(RepositoryModule.attendanceRepository),
    private val enrollFaceUseCase: EnrollFaceUseCase = EnrollFaceUseCase(RepositoryModule.employeeRepository),
    private val employeeRepository: EmployeeRepository = RepositoryModule.employeeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AttendanceUiEffect>()
    val uiEffect: SharedFlow<AttendanceUiEffect> = _uiEffect.asSharedFlow()

    private val livenessChecker = LivenessChecker()

    private var bitmapProvider: (() -> Bitmap?)? = null

    fun setBitmapProvider(provider: () -> Bitmap?) {
        this.bitmapProvider = provider
    }

    private var clockJob: Job? = null
    private var autoDismissJob: Job? = null
    private var verificationJob: Job? = null

    // Đếm số frame liên tiếp khuôn mặt nằm chuẩn vị trí
    private var consecutiveAlignedFrames = 0
    private val requiredAlignedFrames = 5 // ~650ms - 800ms giữ ổn định

    // Đếm số frame không có khuôn mặt để xác nhận người vừa chấm đã rời camera
    private var clearanceNoFaceFrames = 0

    // Lưu trackingId và thời điểm chấm công gần nhất để chống chấm lặp liên tục
    private var lastCheckedTrackingId: Int? = null
    private var lastCheckedTimestamp: Long = 0L
    private val duplicateCooldownMs = 15000L // 15 giây không chấm lại cùng 1 người nếu chưa rời camera

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
            is AttendanceUiEvent.OnSwitchCamera -> {
                consecutiveAlignedFrames = 0
                _uiState.update { current ->
                    current.copy(
                        isFrontCamera = !current.isFrontCamera,
                        status = RecognitionStatus.IDLE
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
            is AttendanceUiEvent.OnSelectEmployee -> {
                _uiState.update {
                    it.copy(
                        selectedEmployeeId = event.employeeId,
                        currentScreen = KioskScreen.EMPLOYEE_DETAIL
                    )
                }
                viewModelScope.launch {
                    val result = employeeRepository.getEmployeeById(event.employeeId)
                    val emp = result.getOrNull()
                    if (emp != null) {
                        _uiState.update { it.copy(selectedEmployee = emp) }
                    }
                }


            }
            is AttendanceUiEvent.OnCaptureFaceSample -> {
                _uiState.update {
                    it.copy(
                        capturedFaceScore = event.metrics?.sharpnessPercent ?: 98,
                        capturedBitmap = event.bitmap,
                        faceQualityMetrics = event.metrics,
                        currentScreen = KioskScreen.FACE_REVIEW
                    )
                }
            }
            is AttendanceUiEvent.OnConfirmEnrollment -> {
                viewModelScope.launch {
                    val result = enrollFaceUseCase(event.employeeId, true)
                    val updatedEmployee = result.getOrNull() ?: _uiState.value.selectedEmployee?.copy(hasFaceSample = true)
                    _uiState.update {
                        it.copy(
                            selectedEmployee = updatedEmployee,
                            shouldRefreshEmployeeList = true,
                            currentScreen = KioskScreen.EMPLOYEE_DETAIL
                        )
                    }
                }
            }
            is AttendanceUiEvent.OnRetakeFaceEnrollment -> {
                viewModelScope.launch {
                    employeeRepository.deleteFaceSamples(event.employeeId)
                }
                _uiState.update {
                    it.copy(
                        currentScreen = KioskScreen.FACE_CAPTURE,
                        capturedBitmap = null,
                        shouldRefreshEmployeeList = true
                    )
                }
            }
            is AttendanceUiEvent.OnConsumeRefreshEmployeeList -> {
                _uiState.update { it.copy(shouldRefreshEmployeeList = false) }
            }

            is AttendanceUiEvent.OnToggleRealMode -> {
                resetToIdle()
                _uiState.update { it.copy(isRealMode = event.isReal) }
            }
            // Các trigger mô phỏng kiểm thử độc lập
            is AttendanceUiEvent.OnSimulateSuccess -> {
                simulateSuccess()
            }
            is AttendanceUiEvent.OnSimulateFailure -> {
                simulateFailure()
            }
            is AttendanceUiEvent.OnSimulateIdle -> {
                resetToIdle()
            }
        }
    }

    /**
     * Xử lý frame khuôn mặt theo State Machine:
     * IDLE -> FACE_DETECTED -> ALIGNING -> FACE_STABLE -> VERIFYING -> SUCCESS / FAILURE -> COOLDOWN -> IDLE
     */
    private fun handleFaceFrame(faceData: FaceDetectionData?) {
        val currentStatus = _uiState.value.status

        // 0. Nếu đang trong trạng thái hiển thị kết quả (SUCCESS / DUPLICATE / FAILURE):
        if (currentStatus == RecognitionStatus.SUCCESS ||
            currentStatus == RecognitionStatus.DUPLICATE ||
            currentStatus == RecognitionStatus.FAILURE
        ) {
            // Trường hợp A: Phát hiện người mới bước vào ngay (trackingId khác hẳn người vừa chấm)
            if (faceData?.trackingId != null && faceData.trackingId != lastCheckedTrackingId) {
                logI("Phát hiện người mới bước vào camera (trackingId: ${faceData.trackingId} != $lastCheckedTrackingId) -> Đổi lượt quét ngay lập tức")
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
                // Không return -> rơi xuống luồng dưới để bắt đầu căn chỉnh và nhận diện người mới ngay!
            } else if (faceData == null) {
                // Trường hợp B: Người vừa chấm đã rời khỏi khung hình
                clearanceNoFaceFrames++
                if (clearanceNoFaceFrames >= 2) { // 2 frame (~200ms) không còn mặt trong camera
                    clearanceNoFaceFrames = 0
                    // Cho phép đóng kết quả sớm nếu người cũ đã rời đi
                    if (_uiState.value.autoDismissRemainingSeconds <= 1) {
                        resetToIdle()
                        return
                    }
                }
                return
            } else {
                // Người cũ vẫn đang đứng trước camera xem kết quả
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

        // Kiểm tra cơ chế Face Clearance: Bắt buộc người vừa chấm phải rời khỏi camera
        if (_uiState.value.isWaitingFaceClearance) {
            // Nếu phát hiện người mới (trackingId khác người vừa chấm) -> Mở khóa Face Clearance ngay lập tức!
            if (faceData?.trackingId != null && faceData.trackingId != lastCheckedTrackingId) {
                logI("Người mới xuất hiện trong thời gian chờ clearance (trackingId: ${faceData.trackingId}) -> Mở khóa quét ngay")
                clearanceNoFaceFrames = 0
                _uiState.update {
                    it.copy(
                        isWaitingFaceClearance = false,
                        stabilityProgress = 0f
                    )
                }
                // Rơi xuống luồng dưới để xử lý khuôn mặt người mới ngay lập tức
            } else if (faceData == null) {
                clearanceNoFaceFrames++
                if (clearanceNoFaceFrames >= 2) { // ~200ms không còn mặt trong khung hình là đủ xác nhận đã rời đi
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
                // Vẫn là người cũ chưa rời đi -> Hiển thị thông báo nhường vị trí với status COOLDOWN
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

        // 1. Không có khuôn mặt
        if (faceData == null) {
            consecutiveAlignedFrames = 0
            livenessChecker.reset()
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.IDLE,
                    faceData = null,
                    alignmentStatus = FaceAlignmentStatus.NO_FACE,
                    activeGuidance = "Vui lòng nhìn thẳng vào camera",
                    confidenceText = "Đang tìm khuôn mặt...",
                    stabilityProgress = 0f
                )
            }
            return
        }

        // Kiểm tra chống chấm công lặp liên tục cho cùng một trackingId (chỉ chặn nếu là cùng 1 người)
        val now = System.currentTimeMillis()
        if (faceData.trackingId != null &&
            faceData.trackingId == lastCheckedTrackingId &&
            (now - lastCheckedTimestamp) < duplicateCooldownMs
        ) {
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.COOLDOWN,
                    faceData = faceData,
                    alignmentStatus = faceData.alignmentStatus,
                    activeGuidance = "Bạn vừa hoàn tất chấm công. Vui lòng nhường vị trí cho người tiếp theo.",
                    confidenceText = "Đã ghi nhận",
                    stabilityProgress = 0f
                )
            }
            return
        }

        // 2. Có khuôn mặt nhưng chưa đạt chuẩn căn chỉnh (Quá xa, quá gần, lệch, nghiêng đầu, nhiều người)
        if (faceData.alignmentStatus != FaceAlignmentStatus.ALIGNED) {
            consecutiveAlignedFrames = 0
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.ALIGNING,
                    faceData = faceData,
                    alignmentStatus = faceData.alignmentStatus,
                    activeGuidance = faceData.guidanceMessage,
                    confidenceText = when (faceData.alignmentStatus) {
                        FaceAlignmentStatus.MULTIPLE_FACES -> "Nhiều người"
                        FaceAlignmentStatus.TOO_FAR -> "Quá xa"
                        FaceAlignmentStatus.TOO_CLOSE -> "Quá gần"
                        FaceAlignmentStatus.OFF_CENTER -> "Lệch tâm"
                        FaceAlignmentStatus.HEAD_TILTED -> "Nghiêng đầu"
                        else -> "Đang căn chỉnh..."
                    },
                    stabilityProgress = 0f
                )
            }
            return
        }

        // 3. Khuôn mặt đã nằm chuẩn trong khung oval (Face Alignment OK)
        consecutiveAlignedFrames++
        val progress = (consecutiveAlignedFrames.toFloat() / requiredAlignedFrames).coerceIn(0f, 1f)

        if (consecutiveAlignedFrames < requiredAlignedFrames) {
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.FACE_STABLE,
                    faceData = faceData,
                    alignmentStatus = FaceAlignmentStatus.ALIGNED,
                    activeGuidance = "Khuôn mặt chuẩn. Vui lòng giữ yên...",
                    confidenceText = "Đang giữ vị trí (${(progress * 100).toInt()}%)",
                )
            }
        } else {
            // Đã giữ yên ổn định đủ thời gian -> Chuyển sang VERIFYING
            consecutiveAlignedFrames = 0
            triggerVerification(faceData)
        }
    }

    /**
     * Tiến hành bước VERIFYING: Chấm công bằng nhận diện khuôn mặt qua FaceCheck AI Service.
     */
    private fun triggerVerification(faceData: FaceDetectionData) {
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

            // Ghi nhận trackingId để chống lặp
            faceData.trackingId?.let {
                lastCheckedTrackingId = it
                lastCheckedTimestamp = System.currentTimeMillis()
            }

            if (_uiState.value.isRealMode) {
                val bitmap = bitmapProvider?.invoke()
                if (bitmap != null) {
                    val base64 = bitmap.toBase64Jpeg(quality = 80, maxDimension = 720, recycleSource = true)
                    logI("Gửi ảnh khuôn mặt chấm công lên FaceCheck AI Backend...")
                    val result = attendanceRepository.checkIn(imageBase64 = base64)
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

    private fun simulateSuccess() {
        autoDismissJob?.cancel()
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.VERIFYING,
                    confidenceText = "Đang đối chiếu dữ liệu (Mô phỏng)..."
                )
            }
            delay(600)
            val nowTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.SUCCESS,
                    confidenceText = "Đã khớp: 99.4% (Mô phỏng)",
                    resultData = AttendanceResultData(
                        employeeName = "Nguyễn Văn An",
                        employeeCode = "NV008",
                        department = "Phòng Công nghệ Phần mềm",
                        checkInTime = nowTime,
                        matchScore = 99.4f,
                        attendanceType = "CHECK_IN",
                        attendanceStatus = "Đúng giờ",
                        isDemo = true
                    ),
                    autoDismissRemainingSeconds = 2,
                    isWaitingFaceClearance = true
                )
            }
            _uiEffect.emit(AttendanceUiEffect.PlaySuccessSound)
            startAutoDismissCountdown()
        }
    }

    private fun simulateFailure() {
        autoDismissJob?.cancel()
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.VERIFYING,
                    confidenceText = "Đang đối chiếu dữ liệu (Mô phỏng)..."
                )
            }
            delay(600)
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.FAILURE,
                    confidenceText = "Độ khớp thấp: 24% (Mô phỏng)",
                    failureReason = "Khuôn mặt chưa khớp hồ sơ TECNIX. Vui lòng căn chỉnh lại góc nhìn hoặc liên hệ quản lý.",
                    resultData = null,
                    autoDismissRemainingSeconds = 2
                )
            }
            _uiEffect.emit(AttendanceUiEffect.PlayErrorSound)
            startAutoDismissCountdown()
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
    }
}
