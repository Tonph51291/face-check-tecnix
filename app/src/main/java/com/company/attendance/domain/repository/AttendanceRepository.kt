package com.company.attendance.domain.repository

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.AttendanceRecord

/**
 * Interface định nghĩa các thao tác lưu trữ và gửi dữ liệu chấm công.
 */
interface AttendanceRepository {
    suspend fun submitAttendance(record: AttendanceRecord): Result<AttendanceRecord>
    suspend fun getRecentAttendanceLogs(employeeId: String, limit: Int = 5): Result<List<AttendanceRecord>>
}
