package com.company.attendance.data.remote.mapper

import com.company.attendance.data.remote.dto.EmployeeDto
import com.company.attendance.domain.model.Employee

/**
 * Mapper chuyển đổi từ EmployeeDto sang Employee (Domain Layer).
 * Map 1-1 trực tiếp, không dùng Regex, không cấp phát mảng phụ để tối ưu bộ nhớ.
 */
fun EmployeeDto.toDomain(): Employee = Employee(
    id = id ?: code.orEmpty(),
    name = name.orEmpty(),
    code = code.orEmpty(),
    department = department ?: "Chung",
    position = position ?: "Nhân viên",
    email = email.orEmpty(),
    phone = phone.orEmpty(),
    joinDate = joinDate.orEmpty(),
    status = status ?: "Đang làm việc",
    hasFaceSample = hasFaceSample,
    avatarInitials = name?.trim()?.firstOrNull()?.uppercase() ?: "NV",
    avatarColorIndex = 0
)

fun List<EmployeeDto>.toDomain(): List<Employee> = map { it.toDomain() }
