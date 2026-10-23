package com.company.attendance.presentation.enrollment

import androidx.compose.runtime.Immutable
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.model.EmployeeDetail

/**
 * Typealias đảm bảo tính tương thích ngược cho AttendanceHistoryItem
 */
typealias AttendanceHistoryItem = com.company.attendance.domain.model.AttendanceHistoryItem

/**
 * UI State cho màn hình Chi tiết nhân sự:
 * Gói gọn toàn bộ dữ liệu trong 1 object EmployeeDetail @Immutable để Compose Compiler
 * thực hiện Smart Skip Recomposition, không phân mảnh state và tiết kiệm bộ nhớ RAM.
 */
@Immutable
data class EmployeeDetailUiState(
    val detail: EmployeeDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    // Các trường trợ giúp tính toán nhanh (computed properties)
    val employee: Employee? get() = detail?.employee
    val faceSampleCount: Int get() = detail?.faceSampleCount ?: 0
    val shiftName: String get() = detail?.shift?.name ?: "Ca tiêu chuẩn"
    val shiftTimeRange: String get() = detail?.shift?.timeRange ?: "08:30 – 17:30"
    val allowedLateMinutes: Int get() = detail?.shift?.allowedLateMinutes ?: 15
    val recentAttendance: List<AttendanceHistoryItem> get() = detail?.recentAttendance ?: emptyList()
}
