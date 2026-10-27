package com.company.attendance.data.remote.mapper

import com.company.attendance.data.remote.dto.AttendanceDataDto
import com.company.attendance.domain.model.AttendanceRecord
import com.company.attendance.domain.model.AttendanceStatus
import com.company.attendance.domain.model.AttendanceType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mapper chuyển đổi giữa AttendanceDataDto và AttendanceRecord.
 * Tái sử dụng DateFormat để triệt tiêu việc cấp phát đối tượng lặp lại.
 */
object AttendanceMapper {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun toDomain(
        dto: AttendanceDataDto,
        serverMessage: String? = null,
        timestampMs: Long = System.currentTimeMillis()
    ): AttendanceRecord {
        val now = Date(timestampMs)
        return AttendanceRecord(
            id = dto.recordId ?: timestampMs.toString(),
            employeeId = dto.employeeId.orEmpty(),
            employeeName = dto.employeeName ?: "Nhân viên",
            employeeCode = dto.employeeId.orEmpty(),
            department = dto.department.orEmpty(),
            timestamp = timestampMs,
            timeFormatted = dto.checkTime ?: synchronized(timeFormat) { timeFormat.format(now) },
            dateFormatted = synchronized(dateFormat) { dateFormat.format(now) },
            matchScore = run {
                val raw = dto.similarityScore ?: 95f
                if (raw <= 1.0f) raw * 100f else raw
            },
            status = AttendanceStatus.fromKey(dto.attendanceStatus),
            attendanceType = AttendanceType.fromKey(dto.attendanceType),
            serverMessage = serverMessage,
            isSynced = true
        )
    }
}
