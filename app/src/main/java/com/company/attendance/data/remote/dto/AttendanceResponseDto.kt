package com.company.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO dữ liệu chi tiết bản ghi chấm công trả về từ server:
 * POST /api/v1/attendance/check-in -> data
 */
data class AttendanceDataDto(
    @SerializedName("recordId")
    val recordId: String? = null,

    @SerializedName("employeeId")
    val employeeId: String? = null,

    @SerializedName("employeeName")
    val employeeName: String? = null,

    @SerializedName("department")
    val department: String? = null,

    @SerializedName("checkTime")
    val checkTime: String? = null,

    @SerializedName("attendanceType")
    val attendanceType: String? = null,

    @SerializedName("attendanceStatus")
    val attendanceStatus: String? = null,

    @SerializedName("similarityScore")
    val similarityScore: Float? = null
)

/**
 * DTO bọc phản hồi chấm công thành công từ AI Service:
 * { "success": true, "code": 200, "message": "Chấm công thành công", "data": { ... } }
 */
data class AttendanceCheckInResponseDto(
    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("code")
    val code: Int = 200,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: AttendanceDataDto? = null,

    @SerializedName("errorCode")
    val errorCode: String? = null
)
