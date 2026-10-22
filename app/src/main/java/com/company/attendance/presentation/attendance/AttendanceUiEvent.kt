package com.company.attendance.presentation.attendance

import com.company.attendance.domain.model.FaceDetectionData

/**
 * Các sự kiện điều khiển của Kiosk
 */
sealed interface AttendanceUiEvent {
    // Sự kiện phân tích khuôn mặt từ CameraX + ML Kit
    data class OnFaceFrameAnalyzed(val faceData: FaceDetectionData?) : AttendanceUiEvent

    data class OnCameraPermissionResult(val granted: Boolean) : AttendanceUiEvent
    data object OnCameraInitialized : AttendanceUiEvent
    data class OnCameraError(val message: String) : AttendanceUiEvent
    data object OnRetryCamera : AttendanceUiEvent
    data object OnSwitchCamera : AttendanceUiEvent

    // Tương tác kết quả
    data object OnDismissResult : AttendanceUiEvent
    data object OnScanAgain : AttendanceUiEvent

    // Mở/đóng cài đặt & chuyển màn hình
    data class OnToggleSettingsDialog(val isVisible: Boolean) : AttendanceUiEvent
    data class OnToggleEmployeeSearch(val isVisible: Boolean) : AttendanceUiEvent
    data class OnNavigateToScreen(val screen: KioskScreen) : AttendanceUiEvent
    data class OnSelectEmployee(val employeeId: String) : AttendanceUiEvent
    data object OnCaptureFaceSample : AttendanceUiEvent
    data class OnConfirmEnrollment(val employeeId: String) : AttendanceUiEvent
    data class OnToggleRealMode(val isReal: Boolean) : AttendanceUiEvent

    // Mô phỏng kiểm thử
    data object OnSimulateSuccess : AttendanceUiEvent
    data object OnSimulateFailure : AttendanceUiEvent
    data object OnSimulateIdle : AttendanceUiEvent
}
