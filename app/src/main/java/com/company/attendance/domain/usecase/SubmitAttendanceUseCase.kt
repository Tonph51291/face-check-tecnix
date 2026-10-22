package com.company.attendance.domain.usecase

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.AttendanceRecord
import com.company.attendance.domain.repository.AttendanceRepository

/**
 * Use case xử lý ghi nhận và gửi bản ghi chấm công lên server.
 */
class SubmitAttendanceUseCase(
    private val attendanceRepository: AttendanceRepository
) {
    suspend operator fun invoke(record: AttendanceRecord): Result<AttendanceRecord> {
        return attendanceRepository.submitAttendance(record)
    }
}
