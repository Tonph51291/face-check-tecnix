package com.company.attendance.domain.model

import androidx.compose.runtime.Immutable

/**
 * Bản ghi hiển thị lịch sử chấm công gần nhất trong hồ sơ nhân viên
 */
@Immutable
data class AttendanceHistoryItem(
    val date: String,
    val time: String,
    val status: String,
    val isGood: Boolean,
    val kioskName: String = "Kiosk Cổng Chính 1"
)

/**
 * Thông tin ca làm việc và quy định chấm công
 */
@Immutable
data class ShiftInfo(
    val name: String = "Ca tiêu chuẩn",
    val timeRange: String = "08:30 - 17:30",
    val allowedLateMinutes: Int = 15,
    val assignedKiosk: String = "Kiosk Cổng 1"
)

/**
 * Mô hình chi tiết nhân sự đầy đủ (Bao gồm hồ sơ, sinh trắc, ca làm việc & lịch sử chấm công)
 */
@Immutable
data class EmployeeDetail(
    val employee: Employee,
    val faceSampleCount: Int = 0,
    val shift: ShiftInfo = ShiftInfo(),
    val recentAttendance: List<AttendanceHistoryItem> = emptyList()
)
