package com.company.attendance.presentation.enrollment

import com.company.attendance.domain.model.Employee

/**
 * Trạng thái giao diện luồng đăng ký nhân sự Kiosk
 */
data class EnrollmentUiState(
    val searchQuery: String = "",
    val selectedDepartment: String = "Tất cả",
    val departments: List<String> = listOf("Tất cả", "Kỹ thuật", "Kinh doanh", "Nhân sự", "Kế toán"),
    val filteredEmployees: List<Employee> = emptyList(),
    val isLoading: Boolean = false,
    val selectedEmployee: Employee? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
