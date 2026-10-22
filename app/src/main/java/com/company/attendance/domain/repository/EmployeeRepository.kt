package com.company.attendance.domain.repository

import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.Employee

/**
 * Interface định nghĩa các thao tác dữ liệu liên quan đến nhân viên trong Domain layer.
 */
interface EmployeeRepository {
    suspend fun getEmployees(): Result<List<Employee>>
    suspend fun searchEmployees(query: String, department: String? = null): Result<List<Employee>>
    suspend fun getEmployeeById(id: String): Result<Employee>
    suspend fun updateFaceEnrollmentStatus(employeeId: String, hasFaceSample: Boolean): Result<Employee>
}
