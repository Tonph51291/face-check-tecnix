package com.company.attendance.data.remote.mapper

import com.company.attendance.data.remote.dto.EmployeeDto
import com.company.attendance.domain.model.AttendanceHistoryItem
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.model.EmployeeDetail
import com.company.attendance.domain.model.ShiftInfo

/**
 * Mapper chuyển đổi từ EmployeeDto (Backend users) sang Employee (Domain Layer).
 */
fun EmployeeDto.toDomain(): Employee {
    val target = unwrap()
    val resolvedPosition = target.position
        ?: target.roles?.firstOrNull()?.name?.let {
            if (it.equals("admin", ignoreCase = true)) "Quản trị viên" else it
        }
        ?: "Nhân viên"

    // Ưu tiên cờ is_face_enrolled từ Backend users API
    val resolvedIsFaceEnrolled = target.isFaceEnrolled
        ?: target.hasFaceSampleSnake
        ?: target.hasFaceSample
        ?: (target.faceSampleCount >= 3)
        ?: ((target.faceSamplesCountSnake ?: 0) >= 3)

    val colorIndex = (target.id?.hashCode() ?: target.name?.hashCode() ?: 0).let { kotlin.math.abs(it) }

    return Employee(
        id = target.id ?: target.effectiveCode,
        name = target.name.orEmpty(),
        code = target.effectiveCode,
        department = target.effectiveDepartment,
        position = resolvedPosition,
        email = target.email.orEmpty(),
        phone = target.phone.orEmpty(),
        joinDate = target.joinDate.orEmpty(),
        status = target.status ?: if (target.isActive == false) "Bị khóa" else "Đang làm việc",
        isFaceEnrolled = resolvedIsFaceEnrolled,
        hasFaceSample = resolvedIsFaceEnrolled,
        avatarUrl = target.avatarUrl.orEmpty(),
        avatarInitials = extractInitials(target.name),
        avatarColorIndex = colorIndex
    )
}

/**
 * Trích xuất 2 chữ cái đầu viết tắt mà không cấp phát String array (không dùng split).
 */
private fun extractInitials(name: String?): String {
    if (name.isNullOrBlank()) return "NV"
    val trimmed = name.trim()
    val spaceIdx = trimmed.lastIndexOf(' ')
    return if (spaceIdx > 0 && spaceIdx < trimmed.length - 1) {
        "${trimmed.first().uppercaseChar()}${trimmed[spaceIdx + 1].uppercaseChar()}"
    } else {
        trimmed.take(2).uppercase()
    }
}

/**
 * Tối ưu hóa: Dùng vòng lặp index với ArrayList(size) thay vì .map {} để tránh cấp phát Iterator lãng phí.
 */
fun List<EmployeeDto>.toDomain(): List<Employee> {
    if (isEmpty()) return emptyList()
    val result = ArrayList<Employee>(size)
    for (i in indices) {
        result.add(this[i].toDomain())
    }
    return result
}

/**
 * Mapper chuyển đổi toàn diện EmployeeDto sang EmployeeDetail (kèm Ca làm việc & Lịch sử chấm công).
 */
fun EmployeeDto.toDetail(): EmployeeDetail {
    val target = unwrap()
    val sampleCount = target.faceSamplesCountSnake ?: target.faceSampleCount

    val logs = target.recentAttendance
    val historyItems = if (!logs.isNullOrEmpty()) {
        val list = ArrayList<AttendanceHistoryItem>(logs.size)
        for (i in logs.indices) {
            val log = logs[i]
            val isGood = log.status == "Đúng giờ" || log.status == "Hợp lệ" || log.status == "ON_TIME"
            val timeStr = when {
                !log.checkIn.isNullOrBlank() -> log.checkIn
                !log.checkOut.isNullOrBlank() -> log.checkOut
                else -> "--:--"
            }
            list.add(
                AttendanceHistoryItem(
                    date = log.date.orEmpty(),
                    time = timeStr,
                    status = log.status ?: "Đúng giờ",
                    isGood = isGood
                )
            )
        }
        list
    } else {
        emptyList()
    }

    return EmployeeDetail(
        employee = target.toDomain(),
        faceSampleCount = sampleCount,
        shift = ShiftInfo(
            name = target.shift?.name ?: "Ca hành chính",
            timeRange = target.shift?.timeRange ?: "08:00 – 17:30",
            allowedLateMinutes = target.shift?.allowedLateMinutes ?: 15
        ),
        recentAttendance = historyItems
    )
}
