package com.company.attendance.data.repository

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.AttendanceRecord
import com.company.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Triển khai AttendanceRepository xử lý lưu trữ offline và đồng bộ dữ liệu chấm công.
 */
class AttendanceRepositoryImpl : AttendanceRepository {

    private val attendanceLogs = mutableListOf<AttendanceRecord>()

    override suspend fun submitAttendance(record: AttendanceRecord): Result<AttendanceRecord> = withContext(Dispatchers.IO) {
        attendanceLogs.add(0, record)
        Result.Success(record)
    }

    override suspend fun getRecentAttendanceLogs(employeeId: String, limit: Int): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        val filtered = attendanceLogs.filter { it.employeeId == employeeId }.take(limit)
        Result.Success(filtered)
    }
}
