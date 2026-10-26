package com.company.attendance.data.repository
 
import androidx.collection.LruCache
import com.company.attendance.core.logging.logApi
import com.company.attendance.core.logging.logD
import com.company.attendance.core.logging.logE
import com.company.attendance.core.result.AppError
import com.company.attendance.core.result.Result
import com.company.attendance.data.remote.api.EmployeeApi
import com.company.attendance.data.remote.dto.ApiErrorResponseDto
import com.company.attendance.data.remote.dto.EnrollFaceDataDto
import com.company.attendance.data.remote.dto.EnrollFaceRequestDto
import com.company.attendance.data.remote.mapper.toDetail
import com.company.attendance.data.remote.mapper.toDomain
import com.company.attendance.di.NetworkModule
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.model.EmployeeDetail
import com.company.attendance.domain.repository.EmployeeRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

/**
 * Triển khai EmployeeRepository kết nối trực tiếp với Backend FaceCheck AI Service.
 * - Dữ liệu thực 100% từ Server.
 * - Tối ưu hóa: Bắt trực tiếp thông điệp tiếng Việt từ API response của Backend (chuẩn OpenAPI).
 * - Tích hợp LRU Cache bộ nhớ đệm (15 hồ sơ nhân sự).
 */
class EmployeeRepositoryImpl(
    private val employeeApi: EmployeeApi = NetworkModule.employeeApi
) : EmployeeRepository {

    private val employeeDetailCache = LruCache<String, EmployeeDetail>(15)
    private val gson = Gson()

    override suspend fun getEmployees(query: String?, department: String?): Result<List<Employee>> =
        getEmployeesPaged(query = query, department = department, page = 1, limit = 50)

    override suspend fun getEmployeesPaged(
        query: String?,
        department: String?,
        page: Int,
        limit: Int
    ): Result<List<Employee>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val apiQuery = query?.trim()?.ifEmpty { null }
            val apiDept = if (department == "Tất cả" || department.isNullOrBlank()) null else department

            val response = employeeApi.getEmployees(apiQuery, apiDept, page, limit)
            if (response.isSuccessful) {
                val list = response.body()?.data?.toDomain().orEmpty()
                logApi("/api/v1/employees", "200 OK", "Nhận ${list.size} nhân sự")
                Result.Success(list)
            } else {
                val errorMsg = response.extractErrorMessage("Lỗi tải danh sách nhân viên")
                logE("Lỗi API getEmployees: $errorMsg")
                Result.Error(AppError.NetworkError(errorMsg))
            }
        }
    }

    override suspend fun searchEmployees(query: String, department: String?): Result<List<Employee>> =
        getEmployees(query = query, department = department)

    override suspend fun getEmployeeById(id: String): Result<Employee> = withContext(Dispatchers.IO) {
        employeeDetailCache.get(id)?.let { return@withContext Result.Success(it.employee) }
        when (val detailResult = getEmployeeDetail(id, forceRefresh = false)) {
            is Result.Success -> Result.Success(detailResult.data.employee)
            is Result.Error -> Result.Error(detailResult.error, detailResult.cause)
        }
    }

    override suspend fun getEmployeeDetail(
        id: String,
        forceRefresh: Boolean
    ): Result<EmployeeDetail> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            employeeDetailCache.get(id)?.let {
                logD("Lấy thông tin chi tiết nhân viên $id từ LRU Cache (0ms)")
                return@withContext Result.Success(it)
            }
        }

        safeApiCall {
            val response = employeeApi.getEmployeeDetail(id)
            val empDto = response.body()?.data
            if (response.isSuccessful && empDto != null) {
                val detail = empDto.toDetail()
                employeeDetailCache.put(id, detail)
                logApi("/api/v1/employees/$id", "200 OK", "Lưu LRU Cache (size=${employeeDetailCache.size()})")
                Result.Success(detail)
            } else {
                val errorMsg = response.extractErrorMessage("Không tìm thấy nhân viên $id")
                Result.Error(AppError.EmployeeNotFound(errorMsg))
            }
        }
    }

    override suspend fun updateFaceEnrollmentStatus(
        employeeId: String,
        hasFaceSample: Boolean
    ): Result<Employee> = withContext(Dispatchers.IO) {
        getEmployeeById(employeeId)
    }

    override suspend fun enrollFaceSample(
        employeeId: String,
        imageBase64: String
    ): Result<EnrollFaceDataDto> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = employeeApi.enrollFace(
                id = employeeId,
                request = EnrollFaceRequestDto(faceImageBase64 = imageBase64)
            )

            val enrollData = response.body()?.data
            if (response.isSuccessful && enrollData != null) {
                employeeDetailCache.remove(employeeId)
                logApi(
                    "/api/v1/employees/$employeeId/enroll-face",
                    "200 OK",
                    "Mẫu: ${enrollData.sampleCount}/${enrollData.requiredSamples}, Đã kích hoạt: ${enrollData.hasFaceSample}"
                )
                Result.Success(enrollData)
            } else {
                // Tận dụng message tiếng Việt chính xác trả về từ Backend AI Service theo OpenAPI
                val errorMsg = response.extractErrorMessage("Không thể lưu mẫu khuôn mặt")
                logE("Lỗi đăng ký mẫu [${response.code()}]: $errorMsg")
                Result.Error(AppError.NetworkError(errorMsg))
            }
        }
    }

    override suspend fun deleteFaceSamples(employeeId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = employeeApi.deleteFaceSamples(employeeId)
            if (response.isSuccessful) {
                employeeDetailCache.remove(employeeId)
            }
            Result.Success(response.isSuccessful)
        }
    }

    /**
     * Trích xuất thông báo lỗi trực tiếp từ JSON response của Server theo chuẩn ApiErrorResponseDto.
     */
    private fun Response<*>.extractErrorMessage(defaultMsg: String): String {
        val raw = errorBody()?.string() ?: return message().ifBlank { "$defaultMsg (${code()})" }
        return try {
            gson.fromJson(raw, ApiErrorResponseDto::class.java)?.message?.takeIf { it.isNotBlank() }
                ?: message().ifBlank { "$defaultMsg (${code()})" }
        } catch (_: Exception) {
            raw.take(120).ifBlank { "$defaultMsg (${code()})" }
        }
    }

    /**
     * Bọc các cuộc gọi API nhằm xử lý thống nhất các lỗi mất mạng IOException hoặc biệt lệ bất ngờ.
     */
    private inline fun <T> safeApiCall(action: () -> Result<T>): Result<T> {
        return try {
            action()
        } catch (e: IOException) {
            logE("Mất kết nối máy chủ AI (${NetworkModule.BASE_URL})", e)
            Result.Error(
                AppError.NetworkError("Không thể kết nối đến máy chủ AI (${NetworkModule.BASE_URL}). Vui lòng kiểm tra server."),
                e
            )
        } catch (e: Exception) {
            logE("Lỗi không xác định khi gọi API", e)
            Result.Error(AppError.Unknown(e.localizedMessage ?: "Lỗi không xác định"), e)
        }
    }
}
