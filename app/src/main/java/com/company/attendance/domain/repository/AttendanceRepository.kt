package com.company.attendance.domain.repository

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.AttendanceRecord

/**
 * Interface định nghĩa các thao tác lưu trữ và gửi dữ liệu chấm công.
 */
interface AttendanceRepository {
    /**
     * Nhận diện khuôn mặt và chấm công trực tiếp qua Backend AI Service (POST /api/v1/attendance/check-in).
     * Nhận diện tự động 1:N chỉ với ảnh chụp, các tham số kỹ thuật được cấu hình nội bộ tại Data layer.
     */
    suspend fun checkIn(imageBase64: String): Result<AttendanceRecord>

    suspend fun submitAttendance(record: AttendanceRecord): Result<AttendanceRecord>
    suspend fun getRecentAttendanceLogs(employeeId: String, limit: Int = 5): Result<List<AttendanceRecord>>
}
