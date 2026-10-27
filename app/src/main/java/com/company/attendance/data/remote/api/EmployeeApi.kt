package com.company.attendance.data.remote.api

import com.company.attendance.data.remote.dto.EmployeeDto
import com.company.attendance.data.remote.dto.EmployeeListResponseDto
import com.company.attendance.data.remote.dto.EnrollFaceRequestDto
import com.company.attendance.data.remote.dto.EnrollFaceResponseDto
import com.company.attendance.data.remote.dto.FaceSamplesResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit Interface định nghĩa các API Nhân sự & Sinh trắc học:
 * Kết nối trực tiếp tới Backend Golang tecnix-office (/users).
 */
interface EmployeeApi {

    /**
     * Lấy danh bạ nhân viên Kiosk:
     * Route: GET /users?search=&department_id=&page=&page_size=
     */
    @GET("users")
    suspend fun getEmployees(
        @Query("search") search: String? = null,
        @Query("department_id") departmentId: Long? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<EmployeeListResponseDto>

    /**
     * Lấy chi tiết nhân viên:
     * Route: GET /users/{id}
     */
    @GET("users/{id}")
    suspend fun getEmployeeDetail(
        @Path("id") id: String
    ): Response<EmployeeDto>

    /**
     * Gửi mẫu ảnh khuôn mặt đăng ký:
     * Route: POST /users/{id}/enroll-face
     */
    @POST("users/{id}/enroll-face")
    suspend fun enrollFace(
        @Path("id") id: String,
        @Body request: EnrollFaceRequestDto
    ): Response<EnrollFaceResponseDto>

    /**
     * Xem danh sách các mẫu khuôn mặt đã lưu:
     * Route: GET /users/{id}/face-samples
     */
    @GET("users/{id}/face-samples")
    suspend fun getFaceSamples(
        @Path("id") id: String
    ): Response<FaceSamplesResponseDto>

    /**
     * Xóa các mẫu khuôn mặt đã lưu của nhân viên để chụp lại từ đầu:
     * Route: DELETE /users/{id}/face-samples
     */
    @DELETE("users/{id}/face-samples")
    suspend fun deleteFaceSamples(
        @Path("id") id: String
    ): Response<com.company.attendance.data.remote.dto.DeleteFaceSamplesResponseDto>
}
