package com.company.attendance.domain.model

import android.graphics.Rect

/**
 * Trạng thái căn chỉnh khuôn mặt so với khung oval Kiosk
 */
enum class FaceAlignmentStatus {
    NO_FACE,            // Không có khuôn mặt
    MULTIPLE_FACES,     // Có nhiều hơn 1 khuôn mặt trong khung
    TOO_FAR,            // Khuôn mặt quá xa (quá nhỏ)
    TOO_CLOSE,          // Khuôn mặt quá gần (quá to)
    OFF_CENTER,         // Khuôn mặt lệch khỏi tâm oval
    HEAD_TILTED,        // Đầu nghiêng hoặc ngửa/cúi quá giới hạn
    ALIGNED             // Khuôn mặt nằm đúng vị trí chuẩn
}

/**
 * Dữ liệu khuôn mặt phát hiện được thời gian thực từ ML Kit
 */
data class FaceDetectionData(
    val boundingBox: Rect,
    val headEulerAngleX: Float, // Pitch: Ngửa / Cúi (- hướng xuống, + hướng lên)
    val headEulerAngleY: Float, // Yaw: Quay trái / Quay phải
    val headEulerAngleZ: Float, // Roll: Nghiêng tai
    val leftEyeOpenProbability: Float? = null,
    val rightEyeOpenProbability: Float? = null,
    val smilingProbability: Float? = null,
    val trackingId: Int? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val rotationDegrees: Int = 0,
    val alignmentStatus: FaceAlignmentStatus = FaceAlignmentStatus.NO_FACE,
    val guidanceMessage: String = "Vui lòng nhìn thẳng vào camera"
) {
    val isAligned: Boolean get() = alignmentStatus == FaceAlignmentStatus.ALIGNED
}
