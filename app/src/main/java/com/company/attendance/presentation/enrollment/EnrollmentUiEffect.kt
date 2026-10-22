package com.company.attendance.presentation.enrollment

import com.company.attendance.domain.model.Employee

/**
 * Hiệu ứng một lần (one-off effects) cho luồng đăng ký nhân sự (Enrollment)
 */
sealed interface EnrollmentUiEffect {
    data class ShowToast(val message: String) : EnrollmentUiEffect
    data class NavigateToDetail(val employee: Employee) : EnrollmentUiEffect
    data class NavigateToCapture(val employee: Employee) : EnrollmentUiEffect
    data object NavigateBack : EnrollmentUiEffect
}
