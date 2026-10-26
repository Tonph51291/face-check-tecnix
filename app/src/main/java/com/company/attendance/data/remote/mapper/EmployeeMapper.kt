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

/**
 * Mapper chuyển đổi toàn diện EmployeeDto sang EmployeeDetail (kèm Ca làm việc & Lịch sử chấm công).
 */
fun EmployeeDto.toDetail(): com.company.attendance.domain.model.EmployeeDetail =
    com.company.attendance.domain.model.EmployeeDetail(
        employee = toDomain(),
        faceSampleCount = faceSampleCount,
        shift = com.company.attendance.domain.model.ShiftInfo(
            name = shift?.name ?: "Ca tiêu chuẩn",
            timeRange = shift?.timeRange ?: "08:30 – 17:30",
            allowedLateMinutes = shift?.allowedLateMinutes ?: 15
        ),
        recentAttendance = recentAttendance?.map { log ->
            val isGood = log.status == "Đúng giờ" || log.status == "Hợp lệ" || log.status == "ON_TIME"
            val timeStr = when {
                !log.checkIn.isNullOrBlank() -> log.checkIn
                !log.checkOut.isNullOrBlank() -> log.checkOut
                else -> "--:--"
            }
            com.company.attendance.domain.model.AttendanceHistoryItem(
                date = log.date.orEmpty(),
                time = timeStr,
                status = log.status ?: "Đúng giờ",
                isGood = isGood
            )
        } ?: emptyList()
    )
