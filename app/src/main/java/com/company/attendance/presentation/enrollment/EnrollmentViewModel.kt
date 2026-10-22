package com.company.attendance.presentation.enrollment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.result.Result
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.usecase.EnrollFaceUseCase
import com.company.attendance.domain.usecase.SearchEmployeeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý luồng đăng ký nhân sự và khuôn mặt theo Base MVVM.
 */
class EnrollmentViewModel(
    private val searchEmployeeUseCase: SearchEmployeeUseCase = SearchEmployeeUseCase(RepositoryModule.employeeRepository),
    private val enrollFaceUseCase: EnrollFaceUseCase = EnrollFaceUseCase(RepositoryModule.employeeRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnrollmentUiState())
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    init {
        loadEmployees()
    }

    fun onEvent(event: EnrollmentUiEvent) {
        when (event) {
            is EnrollmentUiEvent.OnSearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                loadEmployees()
            }
            is EnrollmentUiEvent.OnSelectDepartment -> {
                _uiState.update { it.copy(selectedDepartment = event.department) }
                loadEmployees()
            }
            is EnrollmentUiEvent.OnSelectEmployee -> {
                _uiState.update { it.copy(selectedEmployee = event.employee) }
            }
            is EnrollmentUiEvent.OnConfirmEnrollment -> {
                confirmEnrollment(event.employeeId)
            }
            is EnrollmentUiEvent.OnClearSearch -> {
                _uiState.update { it.copy(searchQuery = "") }
                loadEmployees()
            }
        }
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val query = _uiState.value.searchQuery
            val dept = _uiState.value.selectedDepartment
            when (val result = searchEmployeeUseCase(query = query, department = dept)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            filteredEmployees = result.data,
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

    private fun confirmEnrollment(employeeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = enrollFaceUseCase(employeeId, true)
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedEmployee = result.data,
                            isSaving = false
                        )
                    }
                    loadEmployees()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }
}
