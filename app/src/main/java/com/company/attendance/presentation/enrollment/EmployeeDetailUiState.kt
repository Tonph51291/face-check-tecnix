package com.company.attendance.presentation.enrollment

import androidx.compose.runtime.Immutable
import com.company.attendance.domain.model.Employee

/**
 * Bản ghi hiển thị lịch sử chấm công gần nhất trong thẻ chi tiết nhân viên
 */
@Immutable
data class AttendanceHistoryItem(
    val date: String,
    val time: String,
    val status: String,
    val isGood: Boolean
)

/**
 * UI State cho màn hình Chi tiết nhân viên:
 * Được gắn annotation @Immutable để Compose Compiler tối ưu hóa việc bỏ qua Recomposition (Skip Re-render).
 */
@Immutable
data class EmployeeDetailUiState(
    val employee: Employee? = null,
    val faceSampleCount: Int = 0,
    val shiftName: String = "Ca tiêu chuẩn",
    val shiftTimeRange: String = "08:30 – 17:30",
    val allowedLateMinutes: Int = 15,
    val recentAttendance: List<AttendanceHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
