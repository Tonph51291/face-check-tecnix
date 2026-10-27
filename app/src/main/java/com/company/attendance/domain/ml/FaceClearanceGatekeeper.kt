package com.company.attendance.domain.ml

import com.company.attendance.domain.model.FaceDetectionData

/**
 * Quyết định kiểm soát cổng quét của FaceClearanceGatekeeper
 */
sealed interface ClearanceDecision {
    /**
     * Cho phép tiếp tục luồng quét/chụp tự động bình thường
     */
    data object Allowed : ClearanceDecision

    /**
     * Chặn khuôn mặt: người này đã bị từ chối (409 Conflict) và vẫn đang đứng trước camera.
     * Tuyệt đối không tích lũy progress và không gửi request lên server.
     * @param reason Thông điệp hướng dẫn người dùng
     */
    data class Blocked(val reason: String) : ClearanceDecision

    /**
     * Phát hiện người mới bước vào hoặc người cũ đã rời đi -> Đã tự động mở khóa
     * @param message Thông điệp chào đón người mới
     */
    data class Cleared(val message: String) : ClearanceDecision
}

/**
 * FaceClearanceGatekeeper: Thuật toán kiểm soát chống spam request & xác nhận đổi người (Face Clearance).
 * Tách biệt hoàn toàn khỏi Compose UI, tuân thủ Clean Architecture.
 *
 * Thuật toán hoạt động dựa trên Google ML Kit trackingId:
 * 1. Khi Backend phản hồi 409 (trùng khuôn mặt với nhân viên khác):
 *    -> Gọi [blockFace](trackingId, errorMessage).
 * 2. Khi phân tích từng frame từ camera:
 *    - Nếu cùng trackingId cũ: Chặn ngay lập tức, không cho phép auto-capture hay gửi request lên server.
 *    - Nếu không còn khuôn mặt trong khung hình (clearance threshold ~ 6 frame x 130ms ≈ 0.8s):
 *      -> Xác nhận người cũ đã rời đi -> Tự động mở khóa (Cleared).
 *    - Nếu phát hiện trackingId mới khác với người bị khóa:
 *      -> Nhận diện người mới xuất hiện -> Tự động mở khóa ngay lập tức (Cleared) và cho phép quét người mới.
 */
class FaceClearanceGatekeeper(
    private val requiredClearanceFrames: Int = 6 // ~800ms camera không còn khuôn mặt
) {
    private var blockedTrackingId: Int? = null
    private var isBlocked: Boolean = false
    private var noFaceFramesCount: Int = 0
    private var blockReason: String = "Khuôn mặt đã được đăng ký. Vui lòng rời khỏi khung hình để đổi người."

    /**
     * Khóa khuôn mặt hiện tại khi Backend trả về lỗi 409 Conflict
     */
    fun blockFace(trackingId: Int?, reason: String? = null) {
        blockedTrackingId = trackingId
        isBlocked = true
        noFaceFramesCount = 0
        if (!reason.isNullOrBlank()) {
            blockReason = reason
        }
    }

    /**
     * Đánh giá từng frame camera thời gian thực để đưa ra quyết định
     */
    fun evaluate(faceData: FaceDetectionData?): ClearanceDecision {
        if (!isBlocked) {
            return ClearanceDecision.Allowed
        }

        // Trường hợp 1: Phát hiện trackingId mới khác người bị khóa -> Mở khóa ngay lập tức!
        if (faceData?.trackingId != null && faceData.trackingId != blockedTrackingId) {
            reset()
            return ClearanceDecision.Cleared("Phát hiện người mới. Vui lòng nhìn thẳng camera để quét.")
        }

        // Trường hợp 2: Khung hình trống (không có mặt) -> Đếm clearance frames
        if (faceData == null) {
            noFaceFramesCount++
            if (noFaceFramesCount >= requiredClearanceFrames) {
                reset()
                return ClearanceDecision.Cleared("Sẵn sàng quét khuôn mặt mới.")
            }
            return ClearanceDecision.Blocked("Đang chờ người rời khỏi camera...")
        }

        // Trường hợp 3: Vẫn là khuôn mặt cũ chưa rời đi -> Chặn tuyệt đối!
        noFaceFramesCount = 0
        return ClearanceDecision.Blocked(blockReason)
    }

    val isCurrentlyBlocked: Boolean get() = isBlocked

    /**
     * Mở khóa thủ công hoặc khi người dùng chuyển màn hình / bấm làm mới
     */
    fun reset() {
        blockedTrackingId = null
        isBlocked = false
        noFaceFramesCount = 0
    }
}
