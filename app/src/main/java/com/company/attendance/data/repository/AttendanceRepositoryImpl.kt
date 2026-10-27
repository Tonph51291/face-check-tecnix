package com.company.attendance.data.repository

import com.company.attendance.core.logging.logApi
import com.company.attendance.core.logging.logD
import com.company.attendance.core.logging.logE
import com.company.attendance.core.result.AppError
import com.company.attendance.core.result.Result
import com.company.attendance.data.remote.api.AttendanceApi
import com.company.attendance.data.remote.dto.ApiErrorResponseDto
import com.company.attendance.data.remote.dto.AttendanceCheckInRequestDto
import com.company.attendance.data.remote.mapper.AttendanceMapper
import com.company.attendance.di.NetworkModule
import com.company.attendance.domain.model.AttendanceRecord
import com.company.attendance.domain.repository.AttendanceRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.ArrayDeque
import java.util.UUID

/**
 * Triển khai AttendanceRepository kết nối trực tiếp FaceCheck AI Backend qua Retrofit
 * Xử lý lỗi trực tiếp theo HTTP Status Code chuẩn OpenAPI, bảo vệ RAM bằng Bounded Buffer.
 */
class AttendanceRepositoryImpl(
    private val attendanceApi: AttendanceApi = NetworkModule.attendanceApi
) : AttendanceRepository {

    companion object {
        private const val DEFAULT_DEVICE_SERIAL = "K1"
        // Giới hạn cứng dung lượng trong RAM: Max 50 bản ghi
        private const val MAX_LOGS_LIMIT = 50
    }

    private val attendanceLogs = ArrayDeque<AttendanceRecord>(MAX_LOGS_LIMIT)

    private fun addAttendanceLog(record: AttendanceRecord) {
        synchronized(attendanceLogs) {
            if (attendanceLogs.size >= MAX_LOGS_LIMIT) {
                attendanceLogs.removeLast() // Giải phóng bản ghi cũ nhất khỏi RAM
            }
            attendanceLogs.addFirst(record)
        }
    }

    override suspend fun checkIn(imageBase64: String): Result<AttendanceRecord> = withContext(Dispatchers.IO) {
        try {
            val idempotencyKey = UUID.randomUUID().toString()
            val request = AttendanceCheckInRequestDto(
                capturedImageBase64 = imageBase64,
                deviceSerial = NetworkModule.DEVICE_SERIAL,
                employeeId = null
            )

            val response = attendanceApi.checkIn(request, idempotencyKey)
            logD("CheckIn Response Code: ${response.code()}")
            logApi("/attendance/check-in", "HTTP ${response.code()}", "Success: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val body = response.body()
                val data = body?.data
                if (data != null) {
                    val record = AttendanceMapper.toDomain(
                        dto = data,
                        serverMessage = body.message
                    )
                    addAttendanceLog(record)
                    Result.Success(record)
                } else {
                    Result.Error(AppError.Unknown("Máy chủ không trả về thông tin chi tiết chấm công."))
                }
            } else {
                val rawError = response.errorBody()?.string()
                logE("Lỗi chấm công [HTTP ${response}]: $rawError")
                val parsedError = try {
                    Gson().fromJson(rawError, ApiErrorResponseDto::class.java)
                } catch (_: Exception) {
                    null
                }

                val cleanMessage = parsedError?.detail?.trim()?.ifBlank { null }
                    ?: parsedError?.message?.trim()?.ifBlank { null }
                    ?: response.message().ifBlank { "Lỗi chấm công (${response.code()})" }

                logE("Lỗi chấm công [HTTP ${response.code()}]: $cleanMessage")

                val appError = if (parsedError?.errorCode == "DUPLICATE" || response.code() == 409) {
                    AppError.DuplicateAttendance(cleanMessage)
                } else {
                    AppError.Unknown(cleanMessage)
                }

                Result.Error(appError)
            }
        } catch (e: IOException) {
            logE("Mất kết nối máy chủ AI khi chấm công", e)
            Result.Error(
                AppError.NetworkError("Không thể kết nối đến máy chủ AI (${NetworkModule.BASE_URL})"),
                e
            )
        } catch (e: Exception) {
            logE("Lỗi không xác định khi chấm công", e)
            Result.Error(
                AppError.Unknown(e.localizedMessage ?: "Lỗi chấm công"),
                e
            )
        }
    }

    override suspend fun submitAttendance(record: AttendanceRecord): Result<AttendanceRecord> = withContext(Dispatchers.IO) {
        addAttendanceLog(record)
        Result.Success(record)
    }

    override suspend fun getRecentAttendanceLogs(employeeId: String, limit: Int): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        val filtered = synchronized(attendanceLogs) {
            attendanceLogs.filter { it.employeeId == employeeId }.take(limit)
        }
        Result.Success(filtered)
    }
}
