package com.company.attendance.domain.ml

import android.graphics.Rect
import com.company.attendance.domain.model.FaceAlignmentStatus

class FaceQualityChecker {

    data class QualityEvaluation(
        val status: FaceAlignmentStatus,
        val message: String,
        val isAligned: Boolean,
        val estimatedDistanceText: String
    )

    /**
     * Đánh giá chất lượng và vị trí khuôn mặt so với khung ảnh camera
     *
     * @param boundingBox Tọa độ khuôn mặt phát hiện được
     * @param imageWidth Chiều rộng frame ảnh đã xoay
     * @param imageHeight Chiều cao frame ảnh đã xoay
     * @param angleX Góc ngửa/cúi (Pitch)
     * @param angleY Góc quay trái/phải (Yaw)
     * @param angleZ Góc nghiêng đầu (Roll)
     */
    fun evaluate(
        boundingBox: Rect,
        imageWidth: Int,
        imageHeight: Int,
        angleX: Float,
        angleY: Float,
        angleZ: Float
    ): QualityEvaluation {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return QualityEvaluation(
                status = FaceAlignmentStatus.NO_FACE,
                message = "Đang chờ camera...",
                isAligned = false,
                estimatedDistanceText = "---"
            )
        }

        // Tỷ lệ diện tích khuôn mặt so với frame ảnh
        val faceArea = boundingBox.width().toFloat() * boundingBox.height()
        val imageArea = imageWidth.toFloat() * imageHeight
        val faceCoverageRatio = (faceArea / imageArea).coerceIn(0f, 1f)

        // Tọa độ tâm khuôn mặt chuẩn hóa (0.0 .. 1.0)
        val faceCenterX = boundingBox.centerX().toFloat() / imageWidth
        val faceCenterY = boundingBox.centerY().toFloat() / imageHeight

        // 1. Kiểm tra góc quay đầu tự nhiên (Head Pose - dung sai rộng chuẩn Kiosk)
        if (kotlin.math.abs(angleY) > 24f) {
            return QualityEvaluation(
                status = FaceAlignmentStatus.HEAD_TILTED,
                message = "Vui lòng nhìn về phía camera",
                isAligned = false,
                estimatedDistanceText = "Cần chỉnh góc"
            )
        }

        if (kotlin.math.abs(angleX) > 22f) {
            return QualityEvaluation(
                status = FaceAlignmentStatus.HEAD_TILTED,
                message = "Vui lòng nhìn thẳng về phía camera",
                isAligned = false,
                estimatedDistanceText = "Cần chỉnh góc"
            )
        }

        if (kotlin.math.abs(angleZ) > 20f) {
            return QualityEvaluation(
                status = FaceAlignmentStatus.HEAD_TILTED,
                message = "Vui lòng giữ thẳng đầu",
                isAligned = false,
                estimatedDistanceText = "Cần chỉnh góc"
            )
        }

        // 2. Kiểm tra khoảng cách / kích thước mặt (Hỗ trợ từ 0.5m đến 2m)
        val distanceText: String
        when {
            faceCoverageRatio < 0.04f -> {
                return QualityEvaluation(
                    status = FaceAlignmentStatus.TOO_FAR,
                    message = "Vui lòng bước lại gần camera hơn",
                    isAligned = false,
                    estimatedDistanceText = "Quá xa (> 2m)"
                )
            }
            faceCoverageRatio > 0.65f -> {
                return QualityEvaluation(
                    status = FaceAlignmentStatus.TOO_CLOSE,
                    message = "Vui lòng lùi xa một chút",
                    isAligned = false,
                    estimatedDistanceText = "Quá gần (< 30cm)"
                )
            }
            faceCoverageRatio < 0.12f -> {
                distanceText = "Khoảng cách: ~1.2m"
            }
            faceCoverageRatio > 0.35f -> {
                distanceText = "Khoảng cách: ~50cm"
            }
            else -> {
                distanceText = "Khoảng cách: 70cm (Tốt)"
            }
        }

        // 3. Kiểm tra mép khung hình: Chỉ cần khuôn mặt không bị cắt cụt sát rìa màn hình
        if (faceCenterX !in 0.06f..0.94f || faceCenterY !in 0.06f..0.94f) {
            return QualityEvaluation(
                status = FaceAlignmentStatus.OFF_CENTER,
                message = "Vui lòng đứng vào tầm nhìn của camera",
                isAligned = false,
                estimatedDistanceText = distanceText
            )
        }

        // Mọi chỉ số đạt tiêu chuẩn quét tự do
        return QualityEvaluation(
            status = FaceAlignmentStatus.ALIGNED,
            message = "Đã khóa nét khuôn mặt...",
            isAligned = true,
            estimatedDistanceText = distanceText
        )
    }
}
