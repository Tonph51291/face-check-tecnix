package com.company.attendance.presentation.attendance

import com.company.attendance.domain.model.FaceAlignmentStatus
import com.company.attendance.domain.model.FaceDetectionData

/**
 * State machine quy chuẩn cho Kiosk Chấm công TECNIX:
 * IDLE -> FACE_DETECTED -> ALIGNING -> FACE_STABLE -> VERIFYING -> SUCCESS / FAILURE -> COOLDOWN -> IDLE
 */
enum class RecognitionStatus {
    IDLE,              // Đang chờ khuôn mặt, camera mở sẵn
    FACE_DETECTED,     // Đã phát hiện khuôn mặt trong camera
    ALIGNING,          // Đang hướng dẫn đưa mặt vào đúng vị trí và góc nhìn
    FACE_STABLE,       // Khuôn mặt đã nằm chuẩn và giữ yên ổn định
    VERIFYING,         // Đang thực hiện đối chiếu / nhận diện danh tính
    SUCCESS,           // Xác nhận chấm công thành công
    FAILURE,           // Thất bại (Không khớp / Chưa cấu hình model danh tính)
    COOLDOWN,          // Thời gian hồi sau khi chấm công, tránh lặp lại cùng một người
    DUPLICATE,         // Đã hoàn tất chấm công trong thời gian quy định (Cooldown từ server)
    CAMERA_ERROR,      // Lỗi camera
    PERMISSION_DENIED  // Chưa có quyền camera
}

/**
 * Dữ liệu nhân viên hiển thị khi chấm công
 */
data class AttendanceResultData(
    val employeeName: String = "Nguyễn Văn An",
    val employeeCode: String = "NV008",
    val department: String = "Phòng Công nghệ Phần mềm",
    val checkInTime: String = "08:15:32",
    val matchScore: Float = 99.4f,
    val attendanceType: String = "CHECK_IN", // "CHECK_IN" hoặc "CHECK_OUT"
    val attendanceStatus: String = "Đúng giờ", // "Đúng giờ" hoặc "Đi muộn"
    val serverMessage: String? = null,
    val isDemo: Boolean = false
)

/**
 * Các màn hình trong quy trình Kiosk TECNIX chuẩn Stitch
 */
enum class KioskScreen {
    ATTENDANCE,         // Màn hình 1: Quét mặt chấm công
    EMPLOYEE_SEARCH,    // Màn hình 3: Tìm kiếm nhân viên
    EMPLOYEE_DETAIL,    // Chi tiết nhân viên
    FACE_CAPTURE,       // Màn hình 4A: Chụp mẫu khuôn mặt
    FACE_REVIEW         // Màn hình 4B: Xem lại mẫu khuôn mặt
}

/**
 * Trạng thái toàn diện cho Kiosk TECNIX
 */
data class AttendanceUiState(
    val status: RecognitionStatus = RecognitionStatus.IDLE,
    val hasCameraPermission: Boolean = false,
    val isCameraInitialized: Boolean = false,
    val cameraErrorMessage: String? = null,
    val isFrontCamera: Boolean = false,

    // Điều hướng màn hình
    val currentScreen: KioskScreen = KioskScreen.ATTENDANCE,
    val selectedEmployeeId: String? = null,
    val selectedEmployee: com.company.attendance.domain.model.Employee? = null,
    val capturedFaceScore: Int = 98,
    val capturedBitmap: android.graphics.Bitmap? = null,
    val faceQualityMetrics: com.company.attendance.domain.model.FaceQualityMetrics? = null,
    val shouldRefreshEmployeeList: Boolean = false,

    // Đồng hồ thời gian thực
    val currentTimeString: String = "08:00:00",
    val currentDateString: String = "",
    val shiftName: String = "Ca sáng · 08:00 – 17:30",

    // Dữ liệu phát hiện khuôn mặt thời gian thực (ML Kit On-Device)
    val faceData: FaceDetectionData? = null,
    val alignmentStatus: FaceAlignmentStatus = FaceAlignmentStatus.NO_FACE,
    val activeGuidance: String = "Vui lòng nhìn thẳng vào camera",
    val confidenceText: String = "Đang tìm khuôn mặt...",
    val estimatedDistanceText: String = "---",
    val stabilityProgress: Float = 0f, // 0.0 -> 1.0 tiến trình giữ yên mặt

    // Kết quả xác minh
    val resultData: AttendanceResultData? = null,
    val failureReason: String = "Chưa phát hiện danh tính.",
    val duplicateMessage: String? = null,
    val isWaitingFaceClearance: Boolean = false, // Chờ người vừa chấm rời khỏi camera
    val autoDismissRemainingSeconds: Int = 5,

    // Chế độ & Điều khiển
    val isSettingsDialogVisible: Boolean = false,
    val isEmployeeSearchVisible: Boolean = false,
    val isRealMode: Boolean = true // Mặc định chạy thật 100% với ML Kit
)
