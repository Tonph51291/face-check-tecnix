package com.company.attendance.domain.repository

import com.company.attendance.core.result.Result
import com.company.attendance.data.remote.dto.EnrollFaceDataDto
import com.company.attendance.domain.model.Employee

/**
 * Interface định nghĩa các thao tác dữ liệu liên quan đến nhân viên trong Domain layer.
 */
interface EmployeeRepository {
    suspend fun getEmployees(query: String? = null, department: String? = null): Result<List<Employee>>
    suspend fun getEmployeesPaged(query: String? = null, department: String? = null, page: Int = 1, limit: Int = 20): Result<List<Employee>>
    suspend fun searchEmployees(query: String, department: String? = null): Result<List<Employee>>
    suspend fun getEmployeeById(id: String): Result<Employee>
    suspend fun getEmployeeDetail(id: String, forceRefresh: Boolean = false): Result<com.company.attendance.domain.model.EmployeeDetail>
    suspend fun updateFaceEnrollmentStatus(employeeId: String, hasFaceSample: Boolean): Result<Employee>
    suspend fun enrollFaceSample(employeeId: String, imageBase64: String): Result<EnrollFaceDataDto>
    suspend fun deleteFaceSamples(employeeId: String): Result<Boolean>
}

