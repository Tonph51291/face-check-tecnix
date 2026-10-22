package com.company.attendance.presentation.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.domain.ml.LivenessChecker
import com.company.attendance.domain.model.FaceAlignmentStatus
import com.company.attendance.domain.model.FaceDetectionData
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
import com.company.attendance.core.result.Result
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.model.AttendanceRecord
import com.company.attendance.domain.usecase.EnrollFaceUseCase
import com.company.attendance.domain.usecase.RecognizeFaceUseCase
import com.company.attendance.domain.usecase.SubmitAttendanceUseCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceViewModel(
    private val recognizeFaceUseCase: RecognizeFaceUseCase = RecognizeFaceUseCase(RepositoryModule.employeeRepository),
    private val submitAttendanceUseCase: SubmitAttendanceUseCase = SubmitAttendanceUseCase(RepositoryModule.attendanceRepository),
    private val enrollFaceUseCase: EnrollFaceUseCase = EnrollFaceUseCase(RepositoryModule.employeeRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AttendanceUiEffect>()
    val uiEffect: SharedFlow<AttendanceUiEffect> = _uiEffect.asSharedFlow()

    private val livenessChecker = LivenessChecker()

    private var clockJob: Job? = null
    private var autoDismissJob: Job? = null
    private var verificationJob: Job? = null

    // Đếm số frame liên tiếp khuôn mặt nằm chuẩn vị trí
    private var consecutiveAlignedFrames = 0
    private val requiredAlignedFrames = 5 // ~650ms - 800ms giữ ổn định

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
                        isEmployeeSearchVisible = (event.screen == KioskScreen.EMPLOYEE_SEARCH)
                    )
                }
            }
            is AttendanceUiEvent.OnSelectEmployee -> {
                _uiState.update {
                    it.copy(
                        selectedEmployee = event.employee,
                        currentScreen = KioskScreen.EMPLOYEE_DETAIL
                    )
                }
            }
            is AttendanceUiEvent.OnCaptureFaceSample -> {
                _uiState.update {
                    it.copy(
                        capturedFaceScore = 98,
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
                            currentScreen = KioskScreen.EMPLOYEE_DETAIL
                        )
                    }
                }
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

        // Nếu đang trong trạng thái xử lý kết quả hoặc cooldown, không nhận diện lại
        if (currentStatus == RecognitionStatus.VERIFYING ||
            currentStatus == RecognitionStatus.SUCCESS ||
            currentStatus == RecognitionStatus.FAILURE ||
            currentStatus == RecognitionStatus.COOLDOWN ||
            currentStatus == RecognitionStatus.CAMERA_ERROR ||
            currentStatus == RecognitionStatus.PERMISSION_DENIED
        ) {
            return
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

        // Ghi nhận chỉ số để phân tích liveness vi mô
        livenessChecker.recordFrame(
            leftEyeOpen = faceData.leftEyeOpenProbability,
            rightEyeOpen = faceData.rightEyeOpenProbability,
            yaw = faceData.headEulerAngleY
        )

        // Kiểm tra chống chấm công lặp liên tục cho cùng một trackingId
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
                    stabilityProgress = progress
                )
            }
        } else {
            // Đã giữ yên ổn định đủ thời gian -> Chuyển sang VERIFYING
            consecutiveAlignedFrames = 0
            triggerVerification(faceData)
        }
    }

    /**
     * Tiến hành bước VERIFYING (nhận diện danh tính offline)
     */
    private fun triggerVerification(faceData: FaceDetectionData) {
        verificationJob?.cancel()
        verificationJob = viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    status = RecognitionStatus.VERIFYING,
                    faceData = faceData,
                    activeGuidance = "Đang phân tích khuôn mặt trên thiết bị...",
                    confidenceText = "Đang nhận diện...",
                    stabilityProgress = 1f
                )
            }

            delay(600) // Thời gian suy luận mô phỏng trên frame

            // Đánh giá liveness offline
            val liveness = livenessChecker.evaluate()

            // Ghi nhận trackingId để chống lặp
            faceData.trackingId?.let {
                lastCheckedTrackingId = it
                lastCheckedTimestamp = System.currentTimeMillis()
            }

            // Điều phối qua Domain UseCases: RecognizeFaceUseCase & SubmitAttendanceUseCase
            val result = recognizeFaceUseCase(faceData)
            when (result) {
                is Result.Success -> {
                    val matchResult = result.data
                    if (matchResult.isMatched && matchResult.matchedEmployee != null) {
                        val emp = matchResult.matchedEmployee
                        val nowTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        val nowDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                        // Ghi nhận bản ghi chấm công qua UseCase
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
                                    isDemo = false
                                ),
                                autoDismissRemainingSeconds = 3
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
                                autoDismissRemainingSeconds = 3
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
                            autoDismissRemainingSeconds = 3
                        )
                    }
                    _uiEffect.emit(AttendanceUiEffect.PlayErrorSound)
                    startAutoDismissCountdown()
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
                        isDemo = true
                    ),
                    autoDismissRemainingSeconds = 3
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
                    autoDismissRemainingSeconds = 3
                )
            }
            _uiEffect.emit(AttendanceUiEffect.PlayErrorSound)
            startAutoDismissCountdown()
        }
    }

    private fun startAutoDismissCountdown() {
        autoDismissJob?.cancel()
        autoDismissJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
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
                activeGuidance = "Vui lòng nhìn thẳng vào camera",
                confidenceText = "Đang tìm khuôn mặt...",
                stabilityProgress = 0f,
                resultData = null
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
