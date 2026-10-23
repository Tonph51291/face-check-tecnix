package com.company.attendance.data.repository

import com.company.attendance.core.logging.logApi
import com.company.attendance.core.logging.logD
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

    // Bộ nhớ đệm LRU Cache: Tối đa 15 hồ sơ nhân sự gần nhất, tự động evict hồ sơ cũ nhất để chống phình to RAM
    private val employeeDetailCache = androidx.collection.LruCache<String, com.company.attendance.domain.model.EmployeeDetail>(15)

    override suspend fun getEmployees(query: String?, department: String?): Result<List<Employee>> =
        getEmployeesPaged(query = query, department = department, page = 1, limit = 50)

    override suspend fun getEmployeesPaged(
        query: String?,
        department: String?,
        page: Int,
        limit: Int
    ): Result<List<Employee>> = withContext(Dispatchers.IO) {
        val apiQuery = query?.trim()?.ifEmpty { null }
        val apiDept = if (department == "Tất cả" || department.isNullOrBlank()) null else department

        try {
            // Gọi API backend: GET /api/v1/employees?query=...&department=...&page=...&limit=...
            val response = employeeApi.getEmployees(
                query = apiQuery,
                department = apiDept,
                page = page,
                limit = limit
            )

            logD("API getEmployees (page=$page, limit=$limit): $response")

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

    override suspend fun searchEmployees(query: String, department: String?): Result<List<Employee>> =
        getEmployees(query = query, department = department)

    override suspend fun getEmployeeById(id: String): Result<Employee> = withContext(Dispatchers.IO) {
        // Kiểm tra trong LRU Cache trước để trả kết quả 0ms
        val cached = employeeDetailCache.get(id)
        if (cached != null) {
            return@withContext Result.Success(cached.employee)
        }
        when (val detailResult = getEmployeeDetail(id, forceRefresh = false)) {
            is Result.Success -> Result.Success(detailResult.data.employee)
            is Result.Error -> Result.Error(detailResult.error, detailResult.cause)
        }
    }

    override suspend fun getEmployeeDetail(
        id: String,
        forceRefresh: Boolean
    ): Result<com.company.attendance.domain.model.EmployeeDetail> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = employeeDetailCache.get(id)
            if (cached != null) {
                logD("Lấy thông tin chi tiết nhân viên $id từ LRU Cache (0ms)")
                return@withContext Result.Success(cached)
            }
        }

        try {
            val response = employeeApi.getEmployeeDetail(id)
            if (response.isSuccessful) {
                val empDto = response.body()?.data
                if (empDto != null) {
                    val employee = empDto.toDomain()
                    val shiftDto = empDto.shift
                    val shift = com.company.attendance.domain.model.ShiftInfo(
                        name = shiftDto?.name ?: "Ca tiêu chuẩn",
                        timeRange = shiftDto?.timeRange ?: "08:30 – 17:30",
                        allowedLateMinutes = shiftDto?.allowedLateMinutes ?: 15
                    )
                    val recentLogs = empDto.recentAttendance?.map { log ->
                        val isGood = log.status == "Đúng giờ" || log.status == "Hợp lệ" || log.status == "ON_TIME"
                        val timeStr = when {
                            !log.checkIn.isNullOrBlank() -> log.checkIn
                            !log.checkOut.isNullOrBlank() -> log.checkOut
                            else -> "--:--"
                        }
                        com.company.attendance.domain.model.AttendanceHistoryItem(
                            date = log.date.orEmpty(),
                            time = timeStr,
                            status = log.status ?: "Đúng giờ",
                            isGood = isGood
                        )
                    } ?: emptyList()

                    val detail = com.company.attendance.domain.model.EmployeeDetail(
                        employee = employee,
                        faceSampleCount = empDto.faceSampleCount,
                        shift = shift,
                        recentAttendance = recentLogs
                    )

                    // Lưu vào LRU Cache (tự động evict item cũ khi vượt quá 15 bản ghi)
                    employeeDetailCache.put(id, detail)
                    logApi("/api/v1/employees/$id", "200 OK", "Lưu LRU Cache (size=${employeeDetailCache.size()})")
                    Result.Success(detail)
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
                    employeeDetailCache.remove(employeeId)
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
            if (response.isSuccessful) {
                employeeDetailCache.remove(employeeId)
            }
            Result.Success(response.isSuccessful)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e.localizedMessage ?: "Lỗi khi xóa mẫu khuôn mặt"), e)
        }
    }
}
