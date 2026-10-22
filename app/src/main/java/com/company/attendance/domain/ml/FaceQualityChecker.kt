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

        // 1. Kiểm tra góc quay đầu (Head Pose)
        if (kotlin.math.abs(angleY) > 16f) {
            return QualityEvaluation(
                status = FaceAlignmentStatus.HEAD_TILTED,
                message = "Vui lòng quay mặt nhìn thẳng vào camera",
                isAligned = false,
                estimatedDistanceText = "Cần chỉnh góc"
            )
        }

        if (kotlin.math.abs(angleX) > 18f) {
            return QualityEvaluation(
                status = FaceAlignmentStatus.HEAD_TILTED,
                message = if (angleX > 0) "Vui lòng hạ cằm một chút" else "Vui lòng nâng cằm nhìn thẳng",
                isAligned = false,
                estimatedDistanceText = "Cần chỉnh góc"
            )
        }

        if (kotlin.math.abs(angleZ) > 14f) {
            return QualityEvaluation(
                status = FaceAlignmentStatus.HEAD_TILTED,
                message = "Vui lòng giữ thẳng đầu",
                isAligned = false,
                estimatedDistanceText = "Cần chỉnh góc"
            )
        }

        // 2. Kiểm tra khoảng cách / kích thước mặt
        val distanceText: String
        when {
            faceCoverageRatio < 0.08f -> {
                return QualityEvaluation(
                    status = FaceAlignmentStatus.TOO_FAR,
                    message = "Vui lòng bước lại gần camera hơn",
                    isAligned = false,
                    estimatedDistanceText = "Quá xa (> 1.2m)"
                )
            }
            faceCoverageRatio > 0.50f -> {
                return QualityEvaluation(
                    status = FaceAlignmentStatus.TOO_CLOSE,
                    message = "Vui lòng lùi xa một chút",
                    isAligned = false,
                    estimatedDistanceText = "Quá gần (< 40cm)"
                )
            }
            faceCoverageRatio < 0.14f -> {
                distanceText = "Khoảng cách: ~90cm"
            }
            faceCoverageRatio > 0.35f -> {
                distanceText = "Khoảng cách: ~50cm"
            }
            else -> {
                distanceText = "Khoảng cách: 65cm (Tốt)"
            }
        }

        // 3. Kiểm tra độ lệch tâm (Centering) - Cho phép dung sai 18% so với tâm khung
        val offsetX = kotlin.math.abs(faceCenterX - 0.5f)
        val offsetY = kotlin.math.abs(faceCenterY - 0.48f) // Khung oval kiosk hơi lệch nhẹ lên nửa trên

        if (offsetX > 0.20f || offsetY > 0.22f) {
            val directionHint = buildString {
                append("Vui lòng di chuyển khuôn mặt vào giữa khung oval")
                if (offsetX > 0.20f) {
                    append(if (faceCenterX < 0.5f) " (sang phải)" else " (sang trái)")
                }
            }
            return QualityEvaluation(
                status = FaceAlignmentStatus.OFF_CENTER,
                message = directionHint,
                isAligned = false,
                estimatedDistanceText = distanceText
            )
        }

        // Mọi chỉ số đạt tiêu chuẩn
        return QualityEvaluation(
            status = FaceAlignmentStatus.ALIGNED,
            message = "Khuôn mặt chuẩn. Vui lòng giữ yên...",
            isAligned = true,
            estimatedDistanceText = distanceText
        )
    }
}
