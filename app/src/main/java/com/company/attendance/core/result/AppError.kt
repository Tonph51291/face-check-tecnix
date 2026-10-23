package com.company.attendance.core.result

/**
 * Danh mục lỗi nghiệp vụ và kỹ thuật chuẩn hóa toàn hệ thống Kiosk.
 */
sealed interface AppError {
    val message: String

    // Lỗi nghiệp vụ nhận diện & sinh trắc
    data class FaceNotRecognized(override val message: String = "Khuôn mặt chưa được đăng ký trong hệ thống TECNIX.") : AppError
    data class FaceQualityTooLow(override val message: String = "Chất lượng khuôn mặt chưa đạt tiêu chuẩn nhận diện.") : AppError
    data class LivenessFailed(override val message: String = "Không vượt qua kiểm tra thực thể sống.") : AppError
    data class MultipleFaces(override val message: String = "Phát hiện nhiều khuôn mặt cùng lúc trong khung hình.") : AppError

    // Lỗi dữ liệu nhân viên
    data class EmployeeNotFound(override val message: String = "Không tìm thấy thông tin nhân viên.") : AppError
    data class EmployeeAlreadyEnrolled(override val message: String = "Nhân viên này đã được đăng ký khuôn mặt trước đó.") : AppError

    // Lỗi camera & phần cứng
    data class CameraError(override val message: String = "Không thể kết nối đến camera thiết bị.") : AppError
    data class PermissionDenied(override val message: String = "Quyền truy cập camera bị từ chối.") : AppError

    // Lỗi nghiệp vụ chấm công & ca kíp
    data class DuplicateAttendance(override val message: String = "Bạn vừa hoàn tất chấm công trong thời gian quy định.") : AppError
    data class OutsideAttendanceWindow(override val message: String = "Hiện đang ngoài khung giờ chấm công quy định.") : AppError

    // Lỗi mạng & hệ thống
    data class NetworkError(override val message: String = "Lỗi kết nối mạng đến máy chủ trung tâm.") : AppError
    data class DatabaseError(override val message: String = "Lỗi truy xuất cơ sở dữ liệu cục bộ.") : AppError
    data class Unknown(override val message: String = "Đã xảy ra lỗi không xác định.") : AppError
}
