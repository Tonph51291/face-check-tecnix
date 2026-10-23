package com.company.attendance.data.remote.api

import com.company.attendance.data.remote.dto.AttendanceCheckInRequestDto
import com.company.attendance.data.remote.dto.AttendanceCheckInResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit Interface định nghĩa các endpoint chấm công:
 * Kết nối trực tiếp tới Backend FaceCheck AI Service.
 */
interface AttendanceApi {

    /**
     * Nhận diện khuôn mặt và ghi nhận chấm công tự động trên thiết bị Kiosk.
     * Route: POST /api/v1/attendance/check-in
     */
    @POST("api/v1/attendance/check-in")
    suspend fun checkIn(
        @Body request: AttendanceCheckInRequestDto,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): Response<AttendanceCheckInResponseDto>
}
