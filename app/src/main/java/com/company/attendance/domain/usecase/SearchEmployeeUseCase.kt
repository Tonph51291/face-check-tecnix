package com.company.attendance.domain.usecase

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.repository.EmployeeRepository

/**
 * Use case tìm kiếm và lọc thông tin nhân viên theo tên, mã hoặc phòng ban.
 */
class SearchEmployeeUseCase(
    private val employeeRepository: EmployeeRepository
) {
    suspend operator fun invoke(query: String = "", department: String? = null): Result<List<Employee>> {
        return employeeRepository.searchEmployees(query, department)
    }
}
