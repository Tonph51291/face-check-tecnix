package com.company.attendance.presentation.enrollment

import com.company.attendance.domain.model.Employee

/**
 * Các sự kiện tương tác người dùng trong luồng đăng ký nhân sự
 */
sealed interface EnrollmentUiEvent {
    data class OnSearchQueryChanged(val query: String) : EnrollmentUiEvent
    data class OnSelectDepartment(val department: String) : EnrollmentUiEvent
    data class OnSelectEmployee(val employee: Employee) : EnrollmentUiEvent
    data class OnConfirmEnrollment(val employeeId: String) : EnrollmentUiEvent
    data object OnClearSearch : EnrollmentUiEvent
}
