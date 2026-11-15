package com.company.attendance.domain.usecase

import com.company.attendance.core.result.AppError
import com.company.attendance.core.result.Result
import com.company.attendance.data.remote.dto.EnrollFaceDataDto
import com.company.attendance.domain.repository.EmployeeRepository

/**
 * Use case thực hiện đăng ký đồng thời 3 vector khuôn mặt đa góc (Batch Enrollment)
 * kèm ảnh đại diện cho nhân viên theo OpenAPI Contract của Backend.
 */
class EnrollFaceBatchUseCase(
    private val employeeRepository: EmployeeRepository
) {
    suspend operator fun invoke(
        employeeId: String,
        vectors: List<List<Float>>,
        faceImageBase64: String? = null
    ): Result<EnrollFaceDataDto> {
        if (employeeId.isBlank()) {
            return Result.Error(AppError.EmployeeNotFound("Mã nhân viên không hợp lệ"))
        }
        if (vectors.size < 3) {
            return Result.Error(AppError.InvalidVector("Cần thu thập đủ 3 góc khuôn mặt (Chính diện, Trái, Phải)"))
        }

        return employeeRepository.enrollFaceBatch(
            employeeId = employeeId,
            vectors = vectors.take(3),
            faceImageBase64 = faceImageBase64
        )
    }
}
