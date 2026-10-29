package com.company.attendance.domain.repository

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.AttendanceRecord

/**
 * Interface định nghĩa các thao tác lưu trữ và gửi dữ liệu chấm công.
 */
interface AttendanceRepository {
    /**
     * Nhận diện khuôn mặt và chấm công trực tiếp qua Backend AI Service (POST /attendance/check-in).
     * Gửi vector 512D trích xuất On-Device từ TFLite và ảnh thumbnail đại diện.
     */
    suspend fun checkIn(
        faceVector: List<Float>,
        imageBase64: String? = null
    ): Result<AttendanceRecord>

    suspend fun submitAttendance(record: AttendanceRecord): Result<AttendanceRecord>
    suspend fun getRecentAttendanceLogs(employeeId: String, limit: Int = 5): Result<List<AttendanceRecord>>
}
