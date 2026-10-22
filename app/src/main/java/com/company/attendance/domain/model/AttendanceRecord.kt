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
    val status: String = "Đúng giờ",
    val isSynced: Boolean = false
)
