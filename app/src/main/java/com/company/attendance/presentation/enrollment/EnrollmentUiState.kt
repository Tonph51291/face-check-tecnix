package com.company.attendance.presentation.enrollment

import androidx.compose.runtime.Immutable

/**
 * Trạng thái bộ lọc và cấu hình giao diện Kiosk.
 * Danh sách nhân sự được tách riêng sang Flow<PagingData<Employee>> để tối ưu RAM tuyệt đối.
 */
@Immutable
data class EnrollmentUiState(
    val searchQuery: String = "",
    val selectedDepartment: String = "Tất cả",
    val departments: List<String> = listOf("Tất cả", "Kỹ thuật", "Kinh doanh", "Nhân sự", "Kế toán", "Vận hành")
)
