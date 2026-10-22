package com.company.attendance.domain.usecase

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.repository.EmployeeRepository

/**
 * Use case đăng ký và cập nhật mẫu nhận diện khuôn mặt cho nhân viên.
 */
class EnrollFaceUseCase(
    private val employeeRepository: EmployeeRepository
) {
    suspend operator fun invoke(employeeId: String, hasFaceSample: Boolean = true): Result<Employee> {
        return employeeRepository.updateFaceEnrollmentStatus(employeeId, hasFaceSample)
    }
}
