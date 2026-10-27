package com.company.attendance.data.remote.api

import com.company.attendance.data.remote.dto.AttendanceCheckInRequestDto
import com.company.attendance.data.remote.dto.AttendanceCheckInResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit Interface định nghĩa các endpoint chấm công Kiosk:
 * Route: POST /attendance/check-in
 */
interface AttendanceApi {

    /**
     * Nhận diện khuôn mặt và ghi nhận chấm công tự động trên thiết bị Kiosk.
     */
    @POST("attendance/check-in")
    suspend fun checkIn(
        @Body request: AttendanceCheckInRequestDto,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): Response<AttendanceCheckInResponseDto>
}
