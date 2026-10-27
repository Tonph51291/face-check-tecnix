package com.company.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO dữ liệu phòng ban từ Backend tecnix-office
 */
data class DepartmentDto(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("name")
    val name: String? = null
)

/**
 * DTO vai trò của nhân viên
 */
data class RoleDto(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("is_system")
    val isSystem: Boolean? = null
)

/**
 * DTO phân trang (meta) từ GET /users
 */
data class MetaDto(
    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("page_size")
    val pageSize: Int = 20,

    @SerializedName("total_items")
    val totalItems: Int = 0,

    @SerializedName("total_pages")
    val totalPages: Int = 1
)

/**
 * DTO dữ liệu nhân viên trả về từ Backend:
 * GET /users hoặc GET /users/{id}
 */
data class EmployeeDto(
    @SerializedName("id")
    val rawId: Any? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("code")
    val code: String? = null,

    @SerializedName("department")
    val departmentObj: DepartmentDto? = null,

    @SerializedName("department_name")
    val departmentName: String? = null,

    @SerializedName("position")
    val position: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("joinDate")
    val joinDate: String? = null,

    @SerializedName("is_active")
    val isActive: Boolean? = true,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("is_face_enrolled")
    val isFaceEnrolled: Boolean? = null,

    @SerializedName("hasFaceSample")
    val hasFaceSample: Boolean = false,

    @SerializedName("has_face_sample")
    val hasFaceSampleSnake: Boolean? = null,

    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    @SerializedName("roles")
    val roles: List<RoleDto>? = null,

    @SerializedName("permissions")
    val permissions: List<String>? = null,

    @SerializedName("faceSampleCount")
    val faceSampleCount: Int = 0,

    @SerializedName("face_samples_count")
    val faceSamplesCountSnake: Int? = null,

    @SerializedName("shift")
    val shift: ShiftDto? = null,

    @SerializedName("recentAttendance")
    val recentAttendance: List<RecentAttendanceDto>? = null,

    @SerializedName("data")
    val nestedData: EmployeeDto? = null
) {
    fun unwrap(): EmployeeDto = nestedData?.unwrap() ?: this

    val id: String?
        get() = when (val v = rawId) {
            null -> null
            is Number -> v.toLong().toString()
            is String -> v
            else -> v.toString()
        }

    val effectiveDepartment: String
        get() = departmentObj?.name?.takeIf { it.isNotBlank() }
            ?: departmentName?.takeIf { it.isNotBlank() }
            ?: roles?.firstOrNull()?.name?.takeIf { it.isNotBlank() }?.let {
                if (it.equals("admin", ignoreCase = true)) "Ban Quản trị" else it
            }
            ?: "Phòng ban"

    val effectiveCode: String
        get() = code?.takeIf { it.isNotBlank() }
            ?: id?.let { "NV" + it.padStart(3, '0') }
            ?: "NV001"
}

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
 * DTO response bọc danh sách nhân viên từ Backend tecnix-office:
 * GET /users -> { "items": [...], "current_page": 1, "page_size": 60, "total_pages": 1, "total_items": 2 }
 * hoặc { "data": [...], "meta": { ... } }
 */
data class EmployeeListResponseDto(
    @SerializedName("items")
    val items: List<EmployeeDto>? = null,

    @SerializedName("data")
    val data: List<EmployeeDto>? = null,

    @SerializedName("current_page")
    val currentPage: Int? = null,

    @SerializedName("page_size")
    val pageSize: Int? = null,

    @SerializedName("total_pages")
    val totalPages: Int? = null,

    @SerializedName("total_items")
    val totalItems: Int? = null,

    @SerializedName("meta")
    val meta: MetaDto? = null,

    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("limit")
    val limit: Int = 20,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("errorCode")
    val errorCode: String? = null
) {
    val employees: List<EmployeeDto>
        get() = items ?: data ?: emptyList()
}

/**
 * DTO response chi tiết nhân viên:
 * GET /users/{id}
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

/**
 * DTO gửi yêu cầu đăng ký khuôn mặt:
 * POST /users/{id}/enroll-face
 */
data class EnrollFaceRequestDto(
    @SerializedName("sample_index")
    val sampleIndex: Int? = null,

    @SerializedName("faceImageBase64")
    val faceImageBase64: String? = null
)

data class EnrollFaceDataDto(
    @SerializedName("employeeId")
    val employeeId: String? = null,

    @SerializedName("hasFaceSample")
    val hasFaceSample: Boolean = false,

    @SerializedName("sampleCount")
    val sampleCount: Int = 0,

    @SerializedName("requiredSamples")
    val requiredSamples: Int = 3,

    @SerializedName("message")
    val message: String? = null
)

/**
 * DTO response khi xóa toàn bộ mẫu khuôn mặt: DELETE /users/{id}/face-samples
 */
data class DeleteFaceSamplesResponseDto(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("success")
    val success: Boolean = true
)

/**
 * DTO Response bọc kết quả đăng ký mẫu từ POST /users/{id}/enroll-face:
 * {
 *   "id": 4,
 *   "user_id": 4,
 *   "sample_index": 1,
 *   "samples_enrolled": 1,
 *   "total_samples": 3,
 *   "is_completed": false,
 *   "is_face_enrolled": false,
 *   "photo_path": "storage/faces/4_1.jpg",
 *   "message": "Đã lưu mẫu khuôn mặt số 1 thành công (1/3)"
 * }
 */
data class EnrollFaceResponseDto(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("user_id")
    val userId: Long? = null,

    @SerializedName("sample_index")
    val sampleIndex: Int? = null,

    @SerializedName("samples_enrolled")
    val samplesEnrolled: Int? = null,

    @SerializedName("total_samples")
    val totalSamples: Int? = 3,

    @SerializedName("is_completed")
    val isCompleted: Boolean = false,

    @SerializedName("is_face_enrolled")
    val isFaceEnrolled: Boolean = false,

    @SerializedName("photo_path")
    val photoPath: String? = null,

    @SerializedName("message")
    val message: String? = null,

    // Tương thích ngược nếu backend trả bọc data
    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("data")
    val data: EnrollFaceDataDto? = null
) {
    val effectiveSampleCount: Int
        get() = samplesEnrolled ?: data?.sampleCount ?: sampleIndex ?: 0

    val effectiveIsDone: Boolean
        get() = isCompleted || isFaceEnrolled || (data?.hasFaceSample == true) || (effectiveSampleCount >= (totalSamples ?: 3))
}

/**
 * DTO mẫu khuôn mặt: GET /users/{id}/face-samples
 */
data class FaceSampleItemDto(
    @SerializedName("sample_index")
    val sampleIndex: Int = 1,

    @SerializedName("photo_path")
    val photoPath: String = "",

    @SerializedName("created_at")
    val createdAt: String? = null
)

data class FaceSamplesResponseDto(
    @SerializedName("user_id")
    val userId: Long? = null,

    @SerializedName("samples_enrolled")
    val samplesEnrolled: Int = 0,

    @SerializedName("is_completed")
    val isCompleted: Boolean = false,

    @SerializedName("samples")
    val samples: List<FaceSampleItemDto>? = null
)

/**
 * DTO chi tiết lỗi thẩm định validation 422
 */
data class ValidationErrorDto(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("location")
    val location: String? = null
)

/**
 * DTO chuẩn hóa phản hồi lỗi từ Backend Golang theo chuẩn RFC-7807 (Huma Framework)
 * Hỗ trợ các mã lỗi 400 (Bad Request), 409 (Conflict - trùng mặt), 422 (Unprocessable Entity).
 */
data class Rfc7807ErrorDto(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("status")
    val status: Int? = null,

    @SerializedName("detail")
    val detail: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("errors")
    val errors: List<ValidationErrorDto>? = null
) {
    val userFriendlyMessage: String?
        get() = when {
            !detail.isNullOrBlank() && !errors.isNullOrEmpty() -> {
                val errs = errors.mapNotNull { it.message }.joinToString(", ")
                if (errs.isNotBlank()) "$detail: $errs" else detail
            }
            !detail.isNullOrBlank() -> detail
            !message.isNullOrBlank() -> message
            !title.isNullOrBlank() -> title
            else -> null
        }
}

/**
 * DTO chuẩn hóa phản hồi lỗi từ Backend (tương thích ngược)
 */
data class ApiErrorResponseDto(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("code")
    val code: Int? = null,

    @SerializedName("errorCode")
    val errorCode: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("detail")
    val detail: String? = null
)
