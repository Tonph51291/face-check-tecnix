package com.company.attendance.presentation.enrollment

/**
 * Các sự kiện tương tác người dùng trong màn hình Tìm kiếm nhân sự Kiosk.
 */
sealed interface EnrollmentUiEvent {
    data class OnSearchQueryChanged(val query: String) : EnrollmentUiEvent
    data object OnClearSearch : EnrollmentUiEvent
    data object OnRefresh : EnrollmentUiEvent
}
