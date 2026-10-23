package com.company.attendance.domain.usecase

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.repository.EmployeeRepository

/**
 * Use case tìm kiếm nhân viên theo dạng one-shot list.
 */
@Deprecated("Sử dụng ListEmployeeUseCase trả về PagingData để tối ưu RAM.")
class SearchEmployeeUseCase(
    private val employeeRepository: EmployeeRepository
) {
    suspend operator fun invoke(query: String = "", department: String? = null): Result<List<Employee>> {
        return employeeRepository.getEmployees(query, department)
    }
}
