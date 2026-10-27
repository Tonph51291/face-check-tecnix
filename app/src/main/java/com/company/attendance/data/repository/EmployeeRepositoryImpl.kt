package com.company.attendance.data.repository

import android.util.Log
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
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

/**
 * Triển khai EmployeeRepository kết nối trực tiếp với Backend Golang tecnix-office.
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
            val response = employeeApi.getEmployees(
                search = apiQuery,
                departmentId = null,
                page = page,
                pageSize = limit
            )

            if (response.isSuccessful) {
                val rawEmployees = response.body()?.employees ?: response.body()?.data.orEmpty()
                val domainList = rawEmployees.toDomain()
                val list = if (!department.isNullOrBlank() && department != "Tất cả") {
                    val filtered = ArrayList<Employee>(domainList.size)
                    for (i in domainList.indices) {
                        val emp = domainList[i]
                        if (emp.department.contains(department, ignoreCase = true)) {
                            filtered.add(emp)
                        }
                    }
                    filtered
                } else {
                    domainList
                }
                logApi("/users", "200 OK", "Nhận ${list.size} nhân sự (tổng: ${rawEmployees.size})")
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
            val empDto = response.body()
            if (response.isSuccessful && empDto != null) {
                val detail = empDto.toDetail()
                employeeDetailCache.put(id, detail)
                logApi("/users/$id", "200 OK", "Lưu LRU Cache (size=${employeeDetailCache.size()})")
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
        imageBase64: String,
        sampleIndex: Int?
    ): Result<EnrollFaceDataDto> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = employeeApi.enrollFace(
                id = employeeId,
                request = EnrollFaceRequestDto(
                    sampleIndex = sampleIndex,
                    faceImageBase64 = imageBase64
                )
            )

            val body = response.body()

            if (response.isSuccessful && body != null) {
                employeeDetailCache.remove(employeeId)
                val enrollData = EnrollFaceDataDto(
                    employeeId = body.userId?.toString() ?: body.id?.toString() ?: employeeId,
                    hasFaceSample = body.effectiveIsDone,
                    sampleCount = body.effectiveSampleCount,
                    requiredSamples = body.totalSamples ?: 3,
                    message = body.message
                )
                logApi(
                    "/users/$employeeId/enroll-face",
                    "200 OK",
                    "Mẫu: ${enrollData.sampleCount}/${enrollData.requiredSamples}, Hoàn tất: ${enrollData.hasFaceSample}, Msg: ${body.message}"
                )
                Result.Success(enrollData)
            } else {
                val errorMsg = response.extractErrorMessage("Không thể lưu mẫu khuôn mặt")
                logE("Lỗi đăng ký mẫu [${response.code()}]: $errorMsg")
                val appError = if (response.code() == 409) {
                    AppError.EmployeeAlreadyEnrolled(errorMsg)
                } else {
                    AppError.NetworkError(errorMsg)
                }
                Result.Error(appError)
            }
        }
    }

    override suspend fun deleteFaceSamples(employeeId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = employeeApi.deleteFaceSamples(employeeId)
            if (response.isSuccessful) {
                employeeDetailCache.remove(employeeId)
                logApi("/users/$employeeId/face-samples", "200 OK", "Đã xóa toàn bộ mẫu khuôn mặt")
            }
            Result.Success(response.isSuccessful)
        }
    }

    /**
     * Trích xuất thông báo lỗi trực tiếp từ JSON response của Server theo chuẩn RFC-7807 (Huma Golang backend).
     */
    private fun Response<*>.extractErrorMessage(defaultMsg: String): String {
        val raw = errorBody()?.string() ?: return message().ifBlank { "$defaultMsg (${code()})" }
        return try {
            val rfcError = gson.fromJson(raw, com.company.attendance.data.remote.dto.Rfc7807ErrorDto::class.java)
            rfcError?.userFriendlyMessage?.takeIf { it.isNotBlank() }
                ?: message().ifBlank { "$defaultMsg (${code()})" }
        } catch (_: Exception) {
            raw.take(160).ifBlank { "$defaultMsg (${code()})" }
        }
    }

    /**
     * Bọc các cuộc gọi API nhằm xử lý thống nhất các lỗi mất mạng IOException hoặc biệt lệ bất ngờ.
     */
    private inline fun <T> safeApiCall(action: () -> Result<T>): Result<T> {
        return try {
            action()
        } catch (e: IOException) {
            logE("Mất kết nối máy chủ (${NetworkModule.BASE_URL})", e)
            Result.Error(
                AppError.NetworkError("Không thể kết nối đến máy chủ (${NetworkModule.BASE_URL}). Vui lòng kiểm tra server."),
                e
            )
        } catch (e: Exception) {
            logE("Lỗi không xác định khi gọi API", e)
            Result.Error(AppError.Unknown(e.localizedMessage ?: "Lỗi không xác định"), e)
        }
    }
}
