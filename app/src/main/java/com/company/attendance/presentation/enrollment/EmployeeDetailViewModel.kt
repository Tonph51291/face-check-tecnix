package com.company.attendance.presentation.enrollment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.logging.logApi
import com.company.attendance.core.logging.logD
import com.company.attendance.core.logging.logE
import com.company.attendance.data.remote.api.EmployeeApi
import com.company.attendance.data.remote.mapper.toDomain
import com.company.attendance.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý việc gọi API lấy thông tin chi tiết nhân sự (GET /api/v1/employees/{id}).
 * Nhận ID duy nhất (employeeId: String) để tối ưu hiệu năng truyền nhận giữa các màn hình.
 */
class EmployeeDetailViewModel(
    private val employeeApi: EmployeeApi = NetworkModule.employeeApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeDetailUiState(isLoading = true))
    val uiState: StateFlow<EmployeeDetailUiState> = _uiState.asStateFlow()

    private var currentEmployeeId: String? = null

    fun loadEmployeeDetail(employeeId: String, forceRefresh: Boolean = false) {
        if (employeeId.isBlank()) return
        if (!forceRefresh && currentEmployeeId == employeeId && _uiState.value.employee != null) {
            // Đã có dữ liệu của chính nhân viên này, không cần gọi lại tránh hao tài nguyên
            return
        }
        currentEmployeeId = employeeId
        fetchFromApi(employeeId)
    }


    fun refresh() {
        currentEmployeeId?.let { fetchFromApi(it) }
    }

    private fun fetchFromApi(employeeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            logD("Bắt đầu tải chi tiết nhân viên: /api/v1/employees/$employeeId")

            try {
                val response = employeeApi.getEmployeeDetail(employeeId)
                if (response.isSuccessful) {
                    val dto = response.body()?.data
                    if (dto != null) {
                        logApi("/api/v1/employees/$employeeId", "200 OK", "Mẫu: ${dto.faceSampleCount}")
                        val updatedEmployee = dto.toDomain()
                        val shift = dto.shift
                        val recentLogs = dto.recentAttendance?.map { log ->
                            val isGood = log.status == "Đúng giờ" || log.status == "Hợp lệ" || log.status == "ON_TIME"
                            val timeStr = when {
                                !log.checkIn.isNullOrBlank() -> log.checkIn
                                !log.checkOut.isNullOrBlank() -> log.checkOut
                                else -> "--:--"
                            }
                            AttendanceHistoryItem(
                                date = log.date.orEmpty(),
                                time = timeStr,
                                status = log.status ?: "Đúng giờ",
                                isGood = isGood
                            )
                        } ?: emptyList()

                        _uiState.update {
                            it.copy(
                                employee = updatedEmployee,
                                faceSampleCount = dto.faceSampleCount,
                                shiftName = shift?.name ?: "Ca tiêu chuẩn",
                                shiftTimeRange = shift?.timeRange ?: "08:30 – 17:30",
                                allowedLateMinutes = shift?.allowedLateMinutes ?: 15,
                                recentAttendance = recentLogs,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } else {
                    val err = "Lỗi máy chủ (${response.code()}): ${response.message()}"
                    logE("Lỗi API chi tiết nhân viên: $err")
                    _uiState.update { it.copy(isLoading = false, errorMessage = err) }
                }
            } catch (e: Exception) {
                logE("Lỗi kết nối khi tải chi tiết nhân viên", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }
}
