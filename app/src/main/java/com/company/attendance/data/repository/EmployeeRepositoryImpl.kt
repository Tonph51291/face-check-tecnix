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
import com.google.gson.Gson
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

    override suspend fun enrollFaceSample(
        employeeId: String,
        imageBase64: String
    ): Result<com.company.attendance.data.remote.dto.EnrollFaceDataDto> = withContext(Dispatchers.IO) {
        try {
            val response = employeeApi.enrollFace(
                id = employeeId,
                request = com.company.attendance.data.remote.dto.EnrollFaceRequestDto(faceImageBase64 = imageBase64)
            )

            if (response.isSuccessful) {
                val enrollData = response.body()?.data
                if (enrollData != null) {
                    logApi(
                        "/api/v1/employees/$employeeId/enroll-face",
                        "200 OK",
                        "Mẫu: ${enrollData.sampleCount}/${enrollData.requiredSamples}, Đã kích hoạt: ${enrollData.hasFaceSample}"
                    )
                    Result.Success(enrollData)
                } else {
                    Result.Error(AppError.Unknown("Máy chủ không trả về thông tin mẫu nhận diện."))
                }
            } else {
                val rawError = response.errorBody()?.string()
                val parsedError = try {
                    Gson().fromJson(rawError, com.company.attendance.data.remote.dto.ApiErrorResponseDto::class.java)
                } catch (_: Exception) {
                    null
                }

                val cleanMessage = parsedError?.message?.trim()?.ifBlank { null }
                    ?: when (parsedError?.errorCode) {
                        "FACE_ALREADY_ENROLLED" -> "Khuôn mặt này đã được đăng ký cho một nhân viên khác."
                        "MAX_SAMPLES" -> "Đã đủ 10 mẫu. Vui lòng xóa mẫu cũ để đăng ký lại."
                        "NO_FACE" -> "Không tìm thấy khuôn mặt trong khung hình."
                        "MULTIPLE_FACES" -> "Phát hiện nhiều người. Vui lòng chỉ một người đứng trước camera."
                        "LOW_QUALITY" -> "Ảnh bị mờ hoặc góc nhìn chưa chuẩn. Hãy giữ yên khuôn mặt."
                        "FACE_MISMATCH_WITH_EXISTING" -> "Khuôn mặt không khớp các mẫu đã đăng ký của nhân viên này."
                        "EMPLOYEE_NOT_FOUND" -> "Không tìm thấy thông tin nhân viên."
                        "IMAGE_TOO_LARGE" -> "Ảnh vượt quá dung lượng cho phép."
                        else -> response.message().ifBlank { "Không thể lưu mẫu khuôn mặt (Mã ${response.code()})" }
                    }

                logE("Lỗi đăng ký mẫu [${parsedError?.errorCode ?: response.code()}]: $cleanMessage")
                Result.Error(AppError.NetworkError(cleanMessage))
            }
        } catch (e: IOException) {
            logE("Mất kết nối máy chủ AI khi gửi mẫu khuôn mặt", e)
            Result.Error(
                AppError.NetworkError("Không thể kết nối đến máy chủ AI (${NetworkModule.BASE_URL})"),
                e
            )
        } catch (e: Exception) {
            logE("Lỗi không xác định khi đăng ký khuôn mặt", e)
            Result.Error(
                AppError.Unknown(e.localizedMessage ?: "Lỗi tải mẫu khuôn mặt"),
                e
            )
        }
    }

    override suspend fun deleteFaceSamples(employeeId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = employeeApi.deleteFaceSamples(employeeId)
            Result.Success(response.isSuccessful)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e.localizedMessage ?: "Lỗi khi xóa mẫu khuôn mặt"), e)
        }
    }
}
