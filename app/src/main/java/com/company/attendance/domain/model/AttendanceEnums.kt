package com.company.attendance.domain.model

/**
 * Phân loại ca/loại chấm công nhận diện từ AI Backend:
 * POST /api/v1/attendance/check-in -> attendanceType
 */
enum class AttendanceType(val serverKey: String, val displayName: String) {
    CHECK_IN("CHECK_IN", "Vào ca"),
    CHECK_OUT("CHECK_OUT", "Tan ca"),
    OVERTIME_IN("OVERTIME_IN", "Vào tăng ca"),
    OVERTIME_OUT("OVERTIME_OUT", "Tan tăng ca"),
    BREAK_OUT("BREAK_OUT", "Nghỉ giữa ca"),
    BREAK_IN("BREAK_IN", "Quay lại ca"),
    UNKNOWN("UNKNOWN", "Chấm công");

    companion object {
        fun fromKey(key: String?): AttendanceType {
            if (key.isNullOrBlank()) return CHECK_IN
            return entries.firstOrNull { it.serverKey.equals(key.trim(), ignoreCase = true) } ?: UNKNOWN
        }
    }
}

/**
 * Trạng thái chấp hành giờ giấc chấm công từ AI Backend:
 * POST /api/v1/attendance/check-in -> attendanceStatus
 */
enum class AttendanceStatus(val serverKey: String, val displayName: String) {
    ON_TIME("ON_TIME", "Đúng giờ"),
    LATE("LATE", "Đi muộn"),
    EARLY_LEAVE("EARLY_LEAVE", "Về sớm"),
    OVERTIME("OVERTIME", "Tăng ca"),
    HOLIDAY("HOLIDAY", "Ngày nghỉ"),
    UNKNOWN("UNKNOWN", "Đúng giờ");

    companion object {
        fun fromKey(key: String?): AttendanceStatus {
            if (key.isNullOrBlank()) return ON_TIME
            return entries.firstOrNull { it.serverKey.equals(key.trim(), ignoreCase = true) } ?: UNKNOWN
        }
    }
}
