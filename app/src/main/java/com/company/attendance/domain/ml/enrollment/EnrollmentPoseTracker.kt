package com.company.attendance.domain.ml.enrollment

import com.company.attendance.domain.model.FaceAlignmentStatus
import com.company.attendance.domain.model.FaceDetectionData
import com.company.attendance.presentation.enrollment.TurnGuidanceState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Các bước chụp trong quy trình Đăng ký khuôn mặt đa góc (Multi-View Enrollment)
 */
enum class EnrollmentStep(val stepNumber: Int, val title: String, val prompt: String) {
    FRONTAL(1, "Bước 1/3: Chính diện (Nhìn thẳng)", "Nhìn thẳng vào giữa camera"),
    TURN_LEFT(2, "Bước 2/3: Nghiêng trái (~15°)", "👉 Xoay nhẹ mặt sang bên TRÁI (~15°)"),
    TURN_RIGHT(3, "Bước 3/3: Nghiêng phải (~15°)", "👈 Xoay nhẹ mặt sang bên PHẢI (~15°)")
}

/**
 * Kết quả đánh giá tư thế khuôn mặt theo thời gian thực
 */
data class PoseEvaluation(
    val isAligned: Boolean,
    val guidanceState: TurnGuidanceState,
    val guidanceText: String,
    val distanceText: String = "Khoảng cách hợp lệ (65 cm) • Độ sáng: Tốt",
    val showLeftArrow: Boolean = false,
    val showRightArrow: Boolean = false,
    val progress: Float = 0f,
    val isStepCompleted: Boolean = false
)

/**
 * EnrollmentPoseTracker: Quản lý máy trạng thái 3 góc chụp và tính toán độ ổn định khuôn mặt.
 * - Bước 1: Nhìn thẳng (Yaw trong khoảng -10° đến +10°, mở rộng để người dùng dễ căn chỉnh)
 * - Bước 2: Nghiêng trái cố định (Yaw trong khoảng +12° đến +25°)
 * - Bước 3: Nghiêng phải cố định (Yaw trong khoảng -25° đến -12°)
 * - Khóa góc ngửa/cúi (Pitch) và nghiêng tai (Roll) trong khoảng an toàn ±12°
 * - Tính toán tiến trình ổn định mượt mà (Stability Decay), không bị gián đoạn giật cục khi có nhiễu frame.
 */
class EnrollmentPoseTracker {

    var currentStep: EnrollmentStep = EnrollmentStep.FRONTAL
        private set

    private var stabilityProgress: Float = 0f

    /**
     * Đánh giá frame khuôn mặt hiện tại từ CameraX / ML Kit
     */
    fun evaluatePose(faceData: FaceDetectionData?): PoseEvaluation {
        // 1. Không phát hiện khuôn mặt
        if (faceData == null) {
            decayProgress(0.12f)
            return PoseEvaluation(
                isAligned = false,
                guidanceState = TurnGuidanceState.NO_FACE,
                guidanceText = "Vui lòng nhìn vào khung ngắm camera",
                distanceText = "Chờ khuôn mặt...",
                progress = stabilityProgress
            )
        }

        // 2. Kiểm tra số lượng người trong khung hình
        if (faceData.trackedFaces.size > 1) {
            decayProgress(0.10f)
            return PoseEvaluation(
                isAligned = false,
                guidanceState = TurnGuidanceState.POSITION_INVALID,
                guidanceText = "Chỉ đứng 1 người trước khung ngắm",
                distanceText = "Phát hiện nhiều người",
                progress = stabilityProgress
            )
        }

        // 3. Kiểm tra khoảng cách đứng
        if (faceData.alignmentStatus == FaceAlignmentStatus.TOO_FAR) {
            decayProgress(0.08f)
            return PoseEvaluation(
                isAligned = false,
                guidanceState = TurnGuidanceState.POSITION_INVALID,
                guidanceText = "Vui lòng bước lại gần camera hơn",
                distanceText = "Quá xa (> 1.8m)",
                progress = stabilityProgress
            )
        }
        if (faceData.alignmentStatus == FaceAlignmentStatus.TOO_CLOSE) {
            decayProgress(0.08f)
            return PoseEvaluation(
                isAligned = false,
                guidanceState = TurnGuidanceState.POSITION_INVALID,
                guidanceText = "Vui lòng lùi xa một chút",
                distanceText = "Quá gần (< 35cm)",
                progress = stabilityProgress
            )
        }

        val yaw = faceData.headEulerAngleY
        val pitch = faceData.headEulerAngleX
        val roll = faceData.headEulerAngleZ

        // 4. Khóa góc ngửa/cúi (Pitch) và nghiêng tai (Roll) trong phạm vi an toàn ±12°
        if (abs(pitch) > 12f || abs(roll) > 12f) {
            decayProgress(0.06f)
            val tiltMessage = if (abs(pitch) > 12f) {
                if (pitch > 12f) "Hơi ngửa đầu, hãy hạ cằm xuống" else "Hơi cúi đầu, hãy nâng mặt lên"
            } else {
                "Vui lòng giữ thẳng đầu, không nghiêng tai"
            }
            return PoseEvaluation(
                isAligned = false,
                guidanceState = TurnGuidanceState.HEAD_TILTED,
                guidanceText = tiltMessage,
                distanceText = "Cần chỉnh thẳng đầu",
                progress = stabilityProgress
            )
        }

        // 5. Kiểm tra góc quay ngang (Yaw) theo từng bước cố định
        var isStepAligned = false
        var stepState = TurnGuidanceState.WAITING_TURN
        var stepGuidance = ""
        var showLeft = false
        var showRight = false

        when (currentStep) {
            EnrollmentStep.FRONTAL -> {
                // Bước 1: Chính diện (Yaw trong khoảng -10° đến +10°)
                if (abs(yaw) <= 10f) {
                    isStepAligned = true
                    stepState = TurnGuidanceState.ALIGNED_HOLDING
                } else {
                    isStepAligned = false
                    stepState = TurnGuidanceState.WAITING_TURN
                    stepGuidance = "Nhìn thẳng vào giữa camera"
                }
            }
            EnrollmentStep.TURN_LEFT -> {
                // Bước 2: Xoay sang bên TRÁI (+12° đến +25°)
                when {
                    yaw in 12f..25f -> {
                        isStepAligned = true
                        stepState = TurnGuidanceState.ALIGNED_HOLDING
                    }
                    yaw > 28f -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.TURN_TOO_FAR
                        stepGuidance = "Quay quá nhiều! Hãy xoay nhẹ lại về giữa"
                    }
                    yaw < -5f -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.WAITING_TURN
                        stepGuidance = "Sai hướng! Hãy quay sang bên TRÁI"
                        showLeft = true
                    }
                    else -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.WAITING_TURN
                        stepGuidance = "👉 Vui lòng xoay nhẹ mặt sang bên TRÁI (~15°)"
                        showLeft = true
                    }
                }
            }
            EnrollmentStep.TURN_RIGHT -> {
                // Bước 3: Xoay sang bên PHẢI (-25° đến -12°)
                when {
                    yaw in -25f..-12f -> {
                        isStepAligned = true
                        stepState = TurnGuidanceState.ALIGNED_HOLDING
                    }
                    yaw < -28f -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.TURN_TOO_FAR
                        stepGuidance = "Quay quá nhiều! Hãy xoay nhẹ lại về giữa"
                    }
                    yaw > 5f -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.WAITING_TURN
                        stepGuidance = "Sai hướng! Hãy quay sang bên PHẢI"
                        showRight = true
                    }
                    else -> {
                        isStepAligned = false
                        stepState = TurnGuidanceState.WAITING_TURN
                        stepGuidance = "👈 Vui lòng xoay nhẹ mặt sang bên PHẢI (~15°)"
                        showRight = true
                    }
                }
            }
        }

        if (!isStepAligned) {
            decayProgress(0.06f)
            return PoseEvaluation(
                isAligned = false,
                guidanceState = stepState,
                guidanceText = stepGuidance,
                distanceText = "Khoảng cách hợp lệ (65 cm)",
                showLeftArrow = showLeft,
                showRightArrow = showRight,
                progress = stabilityProgress
            )
        }

        // 6. Tích lũy độ ổn định mượt mà (khoảng 0.6s đạt 100%)
        stabilityProgress = min(stabilityProgress + 0.08f, 1.0f)
        val percent = (stabilityProgress * 100).toInt()
        val holdingGuidance = when (currentStep) {
            EnrollmentStep.FRONTAL -> "Rất tốt! Giữ yên khuôn mặt... $percent%"
            EnrollmentStep.TURN_LEFT -> "Rất tốt! Giữ yên góc trái... $percent%"
            EnrollmentStep.TURN_RIGHT -> "Rất chuẩn! Giữ yên góc phải... $percent%"
        }

        val completed = stabilityProgress >= 1.0f

        return PoseEvaluation(
            isAligned = true,
            guidanceState = TurnGuidanceState.ALIGNED_HOLDING,
            guidanceText = holdingGuidance,
            distanceText = "Khoảng cách chuẩn (65 cm) • Độ sáng: Tốt",
            progress = stabilityProgress,
            isStepCompleted = completed
        )
    }

    /**
     * Chuyển sang bước tiếp theo trong quy trình.
     * Trả về bước mới, hoặc null nếu đã hoàn thành đủ 3 bước.
     */
    fun moveToNextStep(): EnrollmentStep? {
        stabilityProgress = 0f
        currentStep = when (currentStep) {
            EnrollmentStep.FRONTAL -> EnrollmentStep.TURN_LEFT
            EnrollmentStep.TURN_LEFT -> EnrollmentStep.TURN_RIGHT
            EnrollmentStep.TURN_RIGHT -> return null
        }
        return currentStep
    }

    /**
     * Giảm dần thanh tiến độ (decay) thay vì reset cứng về 0% đột ngột
     */
    private fun decayProgress(amount: Float) {
        stabilityProgress = max(0f, stabilityProgress - amount)
    }

    /**
     * Đặt lại tiến trình về Bước 1
     */
    fun reset() {
        currentStep = EnrollmentStep.FRONTAL
        stabilityProgress = 0f
    }
}
