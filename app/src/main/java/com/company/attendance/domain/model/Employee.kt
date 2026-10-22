package com.company.attendance.domain.model

/**
 * Thực thể biểu diễn thông tin nhân viên trong hệ thống chấm công sinh trắc học TECNIX
 */
data class Employee(
    val id: String,
    val name: String,
    val code: String,
    val department: String,
    val position: String = "Kỹ sư Phần mềm",
    val email: String = "an.nguyen@tecnix.vn",
    val phone: String = "0912 345 678",
    val joinDate: String = "15/03/2022",
    val status: String = "Đang làm việc",
    val hasFaceSample: Boolean = false,
    val avatarInitials: String = "",
    val avatarColorIndex: Int = 0
)

val defaultFallbackEmployee = Employee(
    id = "1",
    name = "Nguyễn Văn An",
    code = "NV001",
    department = "Phòng Kỹ thuật & Công nghệ",
    position = "Kỹ sư Trưởng AI",
    email = "an.nguyen@tecnix.vn",
    phone = "0912 345 678",
    joinDate = "15/03/2022",
    hasFaceSample = false,
    avatarInitials = "NA",
    avatarColorIndex = 0
)
