package com.company.attendance.presentation.attendance

/**
 * Hiệu ứng một lần (Single-shot effect) trên Màn hình 1
 */
sealed interface AttendanceUiEffect {
    data class ShowToast(val message: String) : AttendanceUiEffect
    data object PlaySuccessSound : AttendanceUiEffect
    data object PlayErrorSound : AttendanceUiEffect
    data object PlayWarningSound : AttendanceUiEffect
}
