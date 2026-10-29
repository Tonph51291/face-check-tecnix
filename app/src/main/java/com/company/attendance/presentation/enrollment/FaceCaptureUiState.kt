package com.company.attendance.presentation.enrollment

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import com.company.attendance.domain.model.FaceQualityMetrics

/**
 * Trạng thái hướng dẫn xoay đầu theo góc chuẩn xác
 */
enum class TurnGuidanceState {
    NO_FACE,            // Chưa phát hiện khuôn mặt
    WAITING_TURN,       // Đang chờ người dùng xoay đúng hướng (Viền vàng, đứt nét, hiện mũi tên)
    ALIGNED_HOLDING,    // Đã lọt vào đúng vùng góc yêu cầu (Viền xanh lục, nét liền, tích lũy tiến độ)
    TURN_TOO_FAR,       // Xoay quá đà (Viền đỏ cam, nhắc quay lại góc chuẩn)
    HEAD_TILTED,        // Bị ngửa/cúi hoặc nghiêng tai quá mức
    POSITION_INVALID    // Quá gần hoặc quá xa camera
}

/**
 * UI State cho màn hình đăng ký khuôn mặt đa góc & đồng bộ Vector 512D trực tiếp lên Backend.
 */
@Immutable
data class FaceCaptureUiState(
    val employeeId: String = "",
    val currentStep: Int = 1,
    val totalSteps: Int = 3,
    val sampleCount: Int = 0,
    val samplesSuccess: List<Boolean> = listOf(false, false, false),
    val stabilityProgress: Float = 0f,
    val guidanceState: TurnGuidanceState = TurnGuidanceState.WAITING_TURN,
    val guidanceText: String = "Nhìn thẳng vào giữa camera",
    val distanceText: String = "Khoảng cách hợp lệ (65 cm) • Độ sáng: Tốt",
    val isFaceAligned: Boolean = false,
    val isUploading: Boolean = false,
    val uploadStatusMessage: String? = null,
    val isBlockedDuplicate: Boolean = false,
    val errorMessage: String? = null,
    val isConflictDialogVisible: Boolean = false,
    val conflictErrorMessage: String? = null,
    val isMismatchDialogVisible: Boolean = false,
    val mismatchErrorMessage: String? = null,
    val avatarUrl: String? = null,
    val lastCapturedBitmap: Bitmap? = null,
    val lastQualityMetrics: FaceQualityMetrics? = null,
    val showLeftArrow: Boolean = false,
    val showRightArrow: Boolean = false
) {
    val stepTitle: String
        get() = when (currentStep) {
            1 -> "Bước 1/3: Chụp chính diện (Nhìn thẳng)"
            2 -> "Bước 2/3: Nghiêng nhẹ sang trái (~15°)"
            3 -> "Bước 3/3: Nghiêng nhẹ sang phải (~15°)"
            else -> "Đang hoàn tất đăng ký"
        }
}
