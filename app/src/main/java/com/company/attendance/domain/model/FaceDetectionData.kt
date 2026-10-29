package com.company.attendance.domain.model

import android.graphics.Rect
import kotlin.math.abs

/**
 * Trạng thái căn chỉnh khuôn mặt so với khung Kiosk
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
 * Thông tin chi tiết của từng khuôn mặt được theo dõi thời gian thực
 */
data class FaceTrackItem(
    val boundingBox: Rect,
    val trackingId: Int? = null,
    val headEulerAngleX: Float = 0f, // Pitch: Ngửa / Cúi (- hướng xuống, + hướng lên)
    val headEulerAngleY: Float = 0f, // Yaw: Quay trái / Quay phải
    val headEulerAngleZ: Float = 0f, // Roll: Nghiêng tai
    val leftEyeOpenProbability: Float? = null,
    val rightEyeOpenProbability: Float? = null,
    val smilingProbability: Float? = null
) {
    /** Diện tích khuôn mặt (dùng để xác định người đứng gần camera nhất) */
    val area: Int get() = boundingBox.width() * boundingBox.height()

    /** Kiểm tra người dùng có đang hướng mặt về phía camera (ngưỡng tự nhiên ±22 độ) */
    val isLookingStraight: Boolean
        get() = abs(headEulerAngleY) <= 22f && abs(headEulerAngleX) <= 20f && abs(headEulerAngleZ) <= 20f

    /** Kiểm tra đặc trưng khuôn mặt hợp lệ (chống nhận diện nhầm vật thể/tường) */
    val hasValidFaceFeatures: Boolean
        get() {
            val left = leftEyeOpenProbability
            val right = rightEyeOpenProbability
            if (left != null && right != null) {
                // Nếu cả hai mắt đều nhắm hoặc xác suất quá thấp (< 0.05) thì nghi ngờ vật thể vô tri
                if (left < 0.05f && right < 0.05f) {
                    return false
                }
            }
            return true
        }

    /** Kiểm tra 2 mắt đang mở (chống chụp lúc chớp mắt để vector đạt độ nét chuẩn) */
    val areEyesOpen: Boolean
        get() {
            val left = leftEyeOpenProbability ?: return true
            val right = rightEyeOpenProbability ?: return true
            return left >= 0.40f && right >= 0.40f
        }
}

/**
 * Dữ liệu khuôn mặt phát hiện được thời gian thực từ ML Kit
 */
data class FaceDetectionData(
    val boundingBox: Rect,
    val allFaces: List<Rect> = listOf(boundingBox),
    val trackedFaces: List<FaceTrackItem> = emptyList(),
    val primaryFace: FaceTrackItem? = null,
    val headEulerAngleX: Float = 0f,
    val headEulerAngleY: Float = 0f,
    val headEulerAngleZ: Float = 0f,
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
