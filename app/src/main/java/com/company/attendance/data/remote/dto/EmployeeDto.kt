package com.company.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO dữ liệu nhân viên trả về từ FaceCheck AI Service:
 * GET /api/v1/employees hoặc GET /api/v1/employees/{id}
 */
data class EmployeeDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("code")
    val code: String? = null,

    @SerializedName("department")
    val department: String? = null,

    @SerializedName("position")
    val position: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("joinDate")
    val joinDate: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("hasFaceSample")
    val hasFaceSample: Boolean = false,

    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,

    @SerializedName("faceSampleCount")
    val faceSampleCount: Int = 0,

    @SerializedName("shift")
    val shift: ShiftDto? = null,

    @SerializedName("recentAttendance")
    val recentAttendance: List<RecentAttendanceDto>? = null
)

data class ShiftDto(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("timeRange")
    val timeRange: String? = null,

    @SerializedName("allowedLateMinutes")
    val allowedLateMinutes: Int? = null
)

data class RecentAttendanceDto(
    @SerializedName("date")
    val date: String? = null,

    @SerializedName("checkIn")
    val checkIn: String? = null,

    @SerializedName("checkOut")
    val checkOut: String? = null,

    @SerializedName("status")
    val status: String? = null
)

/**
 * DTO response bọc danh sách nhân viên:
 * { "success": true, "total": 8, "page": 1, "limit": 20, "data": [...] }
 */
data class EmployeeListResponseDto(
    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("limit")
    val limit: Int = 20,

    @SerializedName("data")
    val data: List<EmployeeDto>? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("errorCode")
    val errorCode: String? = null
)

/**
 * DTO response chi tiết nhân viên:
 * GET /api/v1/employees/{id} -> { "success": true, "data": { ... } }
 */
data class EmployeeDetailResponseDto(
    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("data")
    val data: EmployeeDto? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("errorCode")
    val errorCode: String? = null
)
