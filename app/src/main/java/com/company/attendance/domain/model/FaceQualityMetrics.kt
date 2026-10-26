package com.company.attendance.domain.model

/**
 * Chỉ số phân tích chất lượng mẫu ảnh sinh trắc học thực tế:
 * - Độ sắc nét (Laplacian Variance): % và trạng thái (Tối ưu / Đạt / Mờ)
 * - Ánh sáng (Luminance & Lux): Giá trị Lux và trạng thái (Đạt / Tối ưu / Thiếu sáng / Chói)
 * - Góc nhìn (Head Pose): Độ lệch góc thực tế (°) và trạng thái (Trực diện / Nghiêng nhẹ / Nghiêng nhiều)
 */
data class FaceQualityMetrics(
    val sharpnessPercent: Int = 96,
    val sharpnessStatus: String = "Tối ưu",
    val lightLux: Int = 320,
    val lightStatus: String = "Đạt",
    val angleDegree: Int = 0,
    val angleStatus: String = "Trực diện",
    val isOverallPass: Boolean = true,
    val recommendationTitle: String = "Ảnh mẫu đã sẵn sàng lưu trữ",
    val recommendationMessage: String = "Ảnh chụp đã vượt qua bài kiểm tra chất lượng thực tế. Các chỉ số sắc nét, ánh sáng và góc nhìn đều đạt chuẩn sinh trắc học."
)
