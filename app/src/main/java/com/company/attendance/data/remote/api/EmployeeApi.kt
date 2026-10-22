package com.company.attendance.data.remote.api

import com.company.attendance.data.remote.dto.EmployeeDetailResponseDto
import com.company.attendance.data.remote.dto.EmployeeListResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit Interface định nghĩa các API liên quan đến Nhân sự:
 * Kết nối tới FaceCheck AI Service.
 */
interface EmployeeApi {

    /**
     * Lấy danh sách nhân viên có hỗ trợ tìm kiếm không dấu và lọc theo phòng ban.
     * Route: GET /api/v1/employees
     */
    @GET("api/v1/employees")
    suspend fun getEmployees(
        @Query("query") query: String? = null,
        @Query("department") department: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<EmployeeListResponseDto>

    /**
     * Lấy chi tiết nhân viên theo ID hoặc mã (NV001).
     * Route: GET /api/v1/employees/{id}
     */
    @GET("api/v1/employees/{id}")
    suspend fun getEmployeeDetail(
        @Path("id") id: String
    ): Response<EmployeeDetailResponseDto>
}
