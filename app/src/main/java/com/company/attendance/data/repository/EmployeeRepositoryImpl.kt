package com.company.attendance.data.repository

import com.company.attendance.core.logging.logApi
import com.company.attendance.core.logging.logE
import com.company.attendance.core.result.AppError
import com.company.attendance.core.result.Result
import com.company.attendance.data.remote.api.EmployeeApi
import com.company.attendance.data.remote.mapper.toDomain
import com.company.attendance.di.NetworkModule
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.repository.EmployeeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Triển khai EmployeeRepository kết nối trực tiếp với Backend FaceCheck AI Service qua Retrofit.
 * 100% dữ liệu thực từ Server; không chứa dữ liệu giả lập (mock data).
 * Nếu BE trả về rỗng, trả về danh sách trống (empty list).
 */
class EmployeeRepositoryImpl(
    private val employeeApi: EmployeeApi = NetworkModule.employeeApi
) : EmployeeRepository {

    override suspend fun getEmployees(): Result<List<Employee>> = searchEmployees(query = "", department = null)

    override suspend fun searchEmployees(query: String, department: String?): Result<List<Employee>> =
        withContext(Dispatchers.IO) {
            val apiQuery = query.trim().ifEmpty { null }
            val apiDept = if (department == "Tất cả" || department.isNullOrBlank()) null else department


            try {
                // Gọi API backend: GET /api/v1/employees?query=...&department=...
                val response = employeeApi.getEmployees(
                    query = apiQuery,
                    department = apiDept,
                    page = 1,
                    limit = 100
                )

                if (response.isSuccessful) {
                    val employeeList = response.body()?.data?.toDomain() ?: emptyList()
                    logApi("/api/v1/employees", "${response.code()} OK", "Nhận ${employeeList.size} nhân sự")
                    Result.Success(employeeList)
                } else {
                    val errorMsg = "Lỗi máy chủ (${response.code()}): ${response.message()}"
                    logE("Lỗi API getEmployees: $errorMsg")
                    Result.Error(AppError.NetworkError(errorMsg))
                }
            } catch (e: IOException) {
                logE("Mất kết nối máy chủ AI (${NetworkModule.BASE_URL})", e)
                Result.Error(
                    AppError.NetworkError(
                        "Không thể kết nối đến máy chủ AI (${NetworkModule.BASE_URL}). Vui lòng kiểm tra server."
                    ),
                    e
                )
            } catch (e: Exception) {
                logE("Lỗi không xác định khi tải dữ liệu", e)
                Result.Error(
                    AppError.Unknown(e.localizedMessage ?: "Lỗi không xác định khi tải dữ liệu"),
                    e
                )
            }
        }

    override suspend fun getEmployeeById(id: String): Result<Employee> = withContext(Dispatchers.IO) {
        try {
            val response = employeeApi.getEmployeeDetail(id)
            if (response.isSuccessful) {
                val empDto = response.body()?.data
                if (empDto != null) {
                    Result.Success(empDto.toDomain())
                } else {
                    Result.Error(AppError.EmployeeNotFound("Không tìm thấy thông tin nhân viên $id"))
                }
            } else {
                Result.Error(AppError.EmployeeNotFound("Không tìm thấy nhân viên ($id): ${response.message()}"))
            }
        } catch (e: IOException) {
            Result.Error(
                AppError.NetworkError("Không thể kết nối đến máy chủ để lấy thông tin nhân viên $id"),
                e
            )
        } catch (e: Exception) {
            Result.Error(
                AppError.Unknown(e.localizedMessage ?: "Lỗi tải thông tin nhân viên"),
                e
            )
        }
    }

    override suspend fun updateFaceEnrollmentStatus(
        employeeId: String,
        hasFaceSample: Boolean
    ): Result<Employee> = withContext(Dispatchers.IO) {
        // Lấy dữ liệu mới nhất từ backend sau khi đăng ký hoặc cập nhật
        getEmployeeById(employeeId)
    }
}
