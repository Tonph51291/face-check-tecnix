package com.company.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object gửi yêu cầu chấm công khuôn mặt lên AI Service theo hợp đồng OpenAPI:
 * POST /api/v1/attendance/check-in
 */
data class AttendanceCheckInRequestDto(
    @SerializedName("capturedImageBase64")
    val capturedImageBase64: String,

    @SerializedName("deviceSerial")
    val deviceSerial: String? = "K1",

    @SerializedName("employeeId")
    val employeeId: String? = null,

    @SerializedName("checkTime")
    val checkTime: String? = null
)
