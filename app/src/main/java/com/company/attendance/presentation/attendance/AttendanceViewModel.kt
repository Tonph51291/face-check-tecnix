package com.company.attendance.presentation.attendance

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.logging.logI
import com.company.attendance.core.result.AppError
import com.company.attendance.core.result.Result
import com.company.attendance.di.MlModule
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.ml.tracker.WalkThroughFaceTracker
import com.company.attendance.domain.model.FaceAlignmentStatus
import com.company.attendance.domain.model.FaceDetectionData
import com.company.attendance.domain.model.FaceTrackItem
import com.company.attendance.domain.repository.AttendanceRepository
import com.company.attendance.domain.usecase.ProcessAttendanceUseCase
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AttendanceViewModel: Nhạc trưởng điều phối tương tác người dùng, máy trạng thái chấm công
 * và luồng dữ liệu giữa View và Domain layer theo chuẩn Clean Architecture.
 */
class AttendanceViewModel @JvmOverloads constructor(
    application: Application,
    private val attendanceRepository: AttendanceRepository = RepositoryModule.attendanceRepository,
    private val faceTracker: WalkThroughFaceTracker = WalkThroughFaceTracker(),
    private val processAttendanceUseCase: ProcessAttendanceUseCase = ProcessAttendanceUseCase(
        faceEmbedder = MlModule.getFaceEmbedder(application.applicationContext),
        attendanceRepository = attendanceRepository
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AttendanceUiEffect>()
    val uiEffect: SharedFlow<AttendanceUiEffect> = _uiEffect.asSharedFlow()

    private var bitmapProvider: (() -> Bitmap?)? = null

    fun setBitmapProvider(provider: () -> Bitmap?) {
        this.bitmapProvider = provider
    }

    private var clockJob: Job? = null
    private var autoDismissJob: Job? = null

    // Bảo vệ phần cứng: Giới hạn tối đa 1 suy luận FaceNet đồng thời để CPU không bị quá tải
    private val inferenceMutex = Mutex()

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
            is AttendanceUiEvent.OnFaceFrameAnalyzed -> handleFaceFrame(event.faceData)
            is AttendanceUiEvent.OnCameraPermissionResult -> handleCameraPermission(event.granted)
            is AttendanceUiEvent.OnCameraInitialized -> handleCameraInitialized()
            is AttendanceUiEvent.OnCameraError -> handleCameraError(event.message)
            is AttendanceUiEvent.OnRetryCamera -> handleRetryCamera()
            is AttendanceUiEvent.OnDismissResult, is AttendanceUiEvent.OnScanAgain -> resetToIdle()
            is AttendanceUiEvent.OnToggleSettingsDialog -> _uiState.update { it.copy(isSettingsDialogVisible = event.isVisible) }
            is AttendanceUiEvent.OnToggleEmployeeSearch -> handleToggleEmployeeSearch(event.isVisible)
            is AttendanceUiEvent.OnNavigateToScreen -> handleNavigateToScreen(event.screen)
            is AttendanceUiEvent.OnToggleRealMode -> handleToggleRealMode(event.isReal)
            is AttendanceUiEvent.OnToggleCamera -> _uiState.update { it.copy(isFrontCamera = event.isFront) }
        }
    }

    private fun handleFaceFrame(faceData: FaceDetectionData?) {
        if (faceData == null || faceData.trackedFaces.isEmpty()) {
            if (_uiState.value.activeTrackedFaces.isNotEmpty() || _uiState.value.detectedFaces.isNotEmpty()) {
                _uiState.update { current ->
                    current.copy(
                        activeTrackedFaces = emptyList(),
                        detectedFaces = emptyList(),
                        primaryFaceBox = null,
                        frameWidth = 0,
                        frameHeight = 0,
                        alignmentStatus = FaceAlignmentStatus.NO_FACE,
                        activeGuidance = "Vui lòng nhìn vào camera",
                        confidenceText = "Sẵn sàng quét mặt",
                        stabilityProgress = 0f
                    )
                }
            }
            return
        }

        // 1. Cập nhật vị trí và trạng thái các khuôn mặt qua FaceTracker
        val trackedFaces = faceData.trackedFaces
        val uiModels = faceTracker.updateTracks(trackedFaces)

        // 2. Tìm ứng viên đủ điều kiện quét mặt (nhìn thẳng, đủ to, chưa bị cooldown)
        val candidate = faceTracker.findEligibleCandidate(trackedFaces)
        if (candidate != null) {
            val trackId = candidate.trackingId ?: candidate.boundingBox.hashCode()
            if (faceTracker.tryClaimForVerification(trackId)) {
                processCandidateAttendance(trackId, candidate, faceData)
            }
        }

        val primaryFace = trackedFaces.maxByOrNull { it.area }
        val primaryBox = primaryFace?.boundingBox ?: faceData.boundingBox

        _uiState.update { current ->
            current.copy(
                activeTrackedFaces = uiModels,
                detectedFaces = trackedFaces.map { it.boundingBox },
                primaryFaceBox = primaryBox,
                frameWidth = faceData.imageWidth,
                frameHeight = faceData.imageHeight,
                alignmentStatus = FaceAlignmentStatus.ALIGNED,
                activeGuidance = if (uiModels.size > 1) "Phát hiện ${uiModels.size} người • Đang quét tự động" else "Chấm công tự động",
                confidenceText = if (uiModels.size > 1) "${uiModels.size} người trong khung" else "Đang bám bắt khuôn mặt"
            )
        }
    }

    private fun processCandidateAttendance(
        trackId: Int,
        face: FaceTrackItem,
        faceData: FaceDetectionData
    ) {
        viewModelScope.launch {
            try {
                inferenceMutex.withLock {
                    if (_uiState.value.isRealMode) {
                        val bitmap = bitmapProvider?.invoke()
                        if (bitmap != null) {
                            val result = processAttendanceUseCase.execute(
                                sourceBitmap = bitmap,
                                targetFace = face,
                                sensorWidth = faceData.imageWidth,
                                sensorHeight = faceData.imageHeight,
                                isFrontCamera = _uiState.value.isFrontCamera
                            )

                            when (result) {
                                is Result.Success -> {
                                    val record = result.data
                                    faceTracker.markSuccess(
                                        trackId = trackId,
                                        employeeId = record.employeeId,
                                        employeeName = record.employeeName,
                                        employeeCode = record.employeeCode,
                                        checkInTime = record.timeFormatted
                                    )
                                    logI("Chấm công thành công: ${record.employeeName} (${record.employeeCode})")
                                    _uiEffect.emit(AttendanceUiEffect.PlaySuccessSound)

                                    _uiState.update { current ->
                                        current.copy(
                                            status = RecognitionStatus.SUCCESS,
                                            resultData = AttendanceResultData(
                                                employeeName = record.employeeName,
                                                employeeCode = record.employeeCode,
                                                department = record.department,
                                                checkInTime = record.timeFormatted,
                                                matchScore = record.matchScore,
                                                attendanceType = record.attendanceType.serverKey,
                                                attendanceStatus = record.status.displayName,
                                                serverMessage = record.serverMessage ?: "Chấm công thành công"
                                            ),
                                            totalCheckInCount = current.totalCheckInCount + 1,
                                            confidenceText = "✓ ${record.employeeName}"
                                        )
                                    }
                                    startAutoDismissCountdown(3)
                                }
                                is Result.Error -> {
                                    val err = result.error
                                    if (err is AppError.DuplicateAttendance) {
                                        faceTracker.markDuplicate(trackId)
                                        _uiEffect.emit(AttendanceUiEffect.PlayWarningSound)
                                    } else {
                                        faceTracker.markFailure(trackId, err.message)
                                        _uiEffect.emit(AttendanceUiEffect.PlayErrorSound)
                                    }
                                }
                            }
                        }
                    } else {
                        // Demo Mode
                        kotlinx.coroutines.delay(400)
                        faceTracker.markSuccess(
                            trackId = trackId,
                            employeeId = "DEMO-01",
                            employeeName = "Nhân viên Demo",
                            employeeCode = "NV999",
                            checkInTime = "08:00:00"
                        )
                        _uiEffect.emit(AttendanceUiEffect.PlaySuccessSound)
                    }
                }
            } finally {
                faceTracker.releaseVerification(trackId)
            }
        }
    }

    private fun handleCameraPermission(granted: Boolean) {
        _uiState.update { current ->
            current.copy(
                hasCameraPermission = granted,
                status = if (granted) RecognitionStatus.IDLE else RecognitionStatus.PERMISSION_DENIED
            )
        }
    }

    private fun handleCameraInitialized() {
        _uiState.update { current ->
            current.copy(
                isCameraInitialized = true,
                cameraErrorMessage = null,
                status = if (current.status == RecognitionStatus.CAMERA_ERROR) RecognitionStatus.IDLE else current.status
            )
        }
    }

    private fun handleCameraError(message: String) {
        _uiState.update { current ->
            current.copy(
                isCameraInitialized = false,
                cameraErrorMessage = message,
                status = RecognitionStatus.CAMERA_ERROR
            )
        }
    }

    private fun handleRetryCamera() {
        _uiState.update { current ->
            current.copy(status = RecognitionStatus.IDLE, cameraErrorMessage = null)
        }
    }

    private fun handleToggleEmployeeSearch(isVisible: Boolean) {
        val nextScreen = if (isVisible) KioskScreen.EMPLOYEE_SEARCH else KioskScreen.ATTENDANCE
        _uiState.update { it.copy(isEmployeeSearchVisible = isVisible, currentScreen = nextScreen) }
    }

    private fun handleNavigateToScreen(screen: KioskScreen) {
        _uiState.update {
            it.copy(
                currentScreen = screen,
                isSettingsDialogVisible = false,
                isEmployeeSearchVisible = (screen == KioskScreen.EMPLOYEE_SEARCH),
                selectedEmployee = if (screen == KioskScreen.ATTENDANCE) null else it.selectedEmployee,
                capturedBitmap = if (screen == KioskScreen.ATTENDANCE || screen == KioskScreen.EMPLOYEE_DETAIL || screen == KioskScreen.FACE_CAPTURE) null else it.capturedBitmap
            )
        }
    }

    private fun handleToggleRealMode(isReal: Boolean) {
        resetToIdle()
        _uiState.update { it.copy(isRealMode = isReal) }
    }

    private fun startAutoDismissCountdown(seconds: Int = 3) {
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
        _uiState.update { current ->
            current.copy(
                status = RecognitionStatus.IDLE,
                resultData = null,
                duplicateMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        autoDismissJob?.cancel()
        faceTracker.clear()
    }
}
