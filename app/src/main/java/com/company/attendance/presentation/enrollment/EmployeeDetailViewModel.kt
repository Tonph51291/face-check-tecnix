package com.company.attendance.presentation.enrollment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.result.Result
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.usecase.GetEmployeeDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý việc tải thông tin chi tiết nhân sự.
 * Tương tác qua GetEmployeeDetailUseCase có tích hợp LRU Cache ở tầng Repository.
 * Phản hồi 0ms khi xem lại nhân viên đã có trong cache, không làm phình to RAM.
 */
class EmployeeDetailViewModel(
    private val getEmployeeDetailUseCase: GetEmployeeDetailUseCase = GetEmployeeDetailUseCase(RepositoryModule.employeeRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeDetailUiState(isLoading = true))
    val uiState: StateFlow<EmployeeDetailUiState> = _uiState.asStateFlow()

    private var currentEmployeeId: String? = null

    fun loadEmployeeDetail(employeeId: String, forceRefresh: Boolean = false) {
        if (employeeId.isBlank()) return
        if (!forceRefresh && currentEmployeeId == employeeId && _uiState.value.detail != null) {
            // Đã có dữ liệu trong bộ nhớ của màn hình hiện tại, không cần gọi lại
            return
        }
        currentEmployeeId = employeeId
        fetchEmployeeDetail(employeeId, forceRefresh)
    }

    fun refresh() {
        currentEmployeeId?.let { fetchEmployeeDetail(it, forceRefresh = true) }
    }

    private fun fetchEmployeeDetail(employeeId: String, forceRefresh: Boolean) {
        viewModelScope.launch {
            // Nếu là làm mới hoặc chưa có dữ liệu thì mới hiển thị trạng thái loading
            if (_uiState.value.detail == null || forceRefresh) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            when (val result = getEmployeeDetailUseCase(employeeId, forceRefresh)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            detail = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }
}
