package com.company.attendance.presentation.enrollment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.result.Result
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.usecase.SearchEmployeeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel chuyên biệt cho màn hình Tìm kiếm nhân sự Kiosk.
 * Tương tác trực tiếp với Backend FaceCheck AI Service qua SearchEmployeeUseCase.
 */
class EnrollmentViewModel(
    private val searchEmployeeUseCase: SearchEmployeeUseCase = SearchEmployeeUseCase(RepositoryModule.employeeRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnrollmentUiState())
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadEmployees(debounceMs = 0)
    }

    fun onEvent(event: EnrollmentUiEvent) {
        when (event) {
            is EnrollmentUiEvent.OnSearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                loadEmployees(debounceMs = 300)
            }
            is EnrollmentUiEvent.OnSelectDepartment -> {
                _uiState.update { it.copy(selectedDepartment = event.department) }
                loadEmployees(debounceMs = 0)
            }
            is EnrollmentUiEvent.OnClearSearch -> {
                _uiState.update { it.copy(searchQuery = "") }
                loadEmployees(debounceMs = 0)
            }
            is EnrollmentUiEvent.OnRefresh -> {
                loadEmployees(debounceMs = 0)
            }
        }
    }

    private fun loadEmployees(debounceMs: Long = 0) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
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
}
