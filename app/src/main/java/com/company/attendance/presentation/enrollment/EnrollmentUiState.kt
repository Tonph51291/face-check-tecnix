package com.company.attendance.presentation.enrollment

import androidx.compose.runtime.Immutable
import com.company.attendance.domain.model.Employee

/**
 * Trạng thái giao diện tìm kiếm nhân sự Kiosk.
 * Được gắn @Immutable để Compose Compiler kích hoạt cơ chế Smart Skip Re-render.
 */
@Immutable
data class EnrollmentUiState(
    val searchQuery: String = "",
    val selectedDepartment: String = "Tất cả",
    val departments: List<String> = listOf("Tất cả", "Kỹ thuật", "Kinh doanh", "Nhân sự", "Kế toán"),
    val filteredEmployees: List<Employee> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
