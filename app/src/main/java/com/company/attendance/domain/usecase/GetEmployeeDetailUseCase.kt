package com.company.attendance.domain.usecase

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.EmployeeDetail
import com.company.attendance.domain.repository.EmployeeRepository

/**
 * Use case lấy thông tin chi tiết nhân sự đầy đủ:
 * Tự động tận dụng LRU In-Memory Cache ở tầng Repository để phản hồi 0ms và chống phình to RAM.
 */
class GetEmployeeDetailUseCase(
    private val employeeRepository: EmployeeRepository
) {
    suspend operator fun invoke(employeeId: String, forceRefresh: Boolean = false): Result<EmployeeDetail> {
        return employeeRepository.getEmployeeDetail(employeeId, forceRefresh)
    }
}
