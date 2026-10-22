package com.company.attendance.domain.model

/**
 * Biểu diễn kết quả so khớp khuôn mặt (độ tương đồng, nhân viên tìm thấy, trạng thái vượt ngưỡng).
 */
data class MatchResult(
    val isMatched: Boolean,
    val matchedEmployee: Employee?,
    val similarityScore: Float,
    val threshold: Float = 0.75f,
    val message: String = if (isMatched) "Khớp danh tính thành công" else "Không tìm thấy khuôn mặt phù hợp"
)
