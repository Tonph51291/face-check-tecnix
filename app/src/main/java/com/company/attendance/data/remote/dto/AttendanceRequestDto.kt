package com.company.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object gửi yêu cầu chấm công khuôn mặt lên Backend:
 * POST /attendance/check-in
 */
data class AttendanceCheckInRequestDto(
    @SerializedName("face_vector")
    val faceVector: List<Float>? = null,

    @SerializedName("capturedImageBase64")
    val capturedImageBase64: String? = null,

    @SerializedName("image_base64")
    val imageBase64: String? = null,

    @SerializedName("deviceSerial")
    val deviceSerial: String? = "KIOSK-01",

    @SerializedName("device_sn")
    val deviceSn: String? = null,

    @SerializedName("employeeId")
    val employeeId: String? = null,

    @SerializedName("checkTime")
    val checkTime: String? = null
)
