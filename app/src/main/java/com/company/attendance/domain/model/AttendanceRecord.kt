package com.company.attendance.domain.model

/**
 * Thực thể biểu diễn bản ghi lịch sử chấm công của nhân viên.
 */
data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val employeeCode: String,
    val department: String,
    val timestamp: Long,
    val timeFormatted: String,
    val dateFormatted: String,
    val matchScore: Float,
    val status: AttendanceStatus = AttendanceStatus.ON_TIME,
    val attendanceType: AttendanceType = AttendanceType.CHECK_IN,
    val serverMessage: String? = null,
    val isSynced: Boolean = false
) {
    val statusText: String get() = status.displayName
    val typeText: String get() = attendanceType.displayName
}
