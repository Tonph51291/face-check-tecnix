package com.company.attendance.data.remote.mapper

import com.company.attendance.data.remote.dto.EmployeeDto
import com.company.attendance.domain.model.Employee

/**
 * Mapper chuyển đổi từ EmployeeDto (Data Layer) sang Employee (Domain Layer).
 */
fun EmployeeDto.toDomain(): Employee {
    val cleanName = name.orEmpty().trim()
    val words = cleanName.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    val initials = when {
        words.size >= 2 -> "${words.first().first().uppercaseChar()}${words.last().first().uppercaseChar()}"
        words.size == 1 -> words.first().take(2).uppercase()
        else -> "NV"
    }

    val hashKey = code ?: cleanName
    val colorIndex = if (hashKey.isNotEmpty()) Math.abs(hashKey.hashCode()) % 4 else 0

    return Employee(
        id = id ?: code ?: "",
        name = cleanName,
        code = code.orEmpty(),
        department = department ?: "Chung",
        position = position ?: "Nhân viên",
        email = email.orEmpty(),
        phone = phone.orEmpty(),
        joinDate = joinDate.orEmpty(),
        status = status ?: "Đang làm việc",
        hasFaceSample = hasFaceSample,
        avatarInitials = initials,
        avatarColorIndex = colorIndex
    )
}

fun List<EmployeeDto>.toDomain(): List<Employee> = map { it.toDomain() }
