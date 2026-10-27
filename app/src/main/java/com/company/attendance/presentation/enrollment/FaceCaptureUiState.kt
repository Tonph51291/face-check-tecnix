package com.company.attendance.presentation.enrollment

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

/**
 * UI State thuần túy cho màn hình đăng ký khuôn mặt đa mẫu tự động (Smart Multi-Shot Auto-Capture).
 * Tách biệt hoàn toàn khỏi các side-effects điều hướng và lifecycle.
 */
@Immutable
data class FaceCaptureUiState(
    val employeeId: String = "",
    val currentStep: Int = 1,
    val totalSteps: Int = 3,
    val sampleCount: Int = 0,
    val stabilityProgress: Float = 0f,
    val guidanceText: String = "Nhìn thẳng vào camera và giữ yên khuôn mặt",
    val distanceText: String = "Khoảng cách hợp lệ (65 cm) • Độ sáng: Tốt",
    val isFaceAligned: Boolean = false,
    val isUploading: Boolean = false,
    val isBlockedDuplicate: Boolean = false,
    val errorMessage: String? = null,
    val lastCapturedBitmap: Bitmap? = null,
    val lastQualityMetrics: com.company.attendance.domain.model.FaceQualityMetrics? = null
) {
    val stepTitle: String
        get() = when (currentStep) {
            1 -> "Bước 1/3: Chụp chính diện"
            2 -> "Bước 2/3: Chụp góc nghiêng nhẹ"
            3 -> "Bước 3/3: Chụp biểu cảm tự nhiên"
            else -> "Đang lưu mẫu khuôn mặt"
        }
}
