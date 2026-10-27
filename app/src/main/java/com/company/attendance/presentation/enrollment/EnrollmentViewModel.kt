package com.company.attendance.presentation.enrollment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.usecase.ListEmployeeUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * ViewModel chuyên biệt cho màn hình Danh sách nhân sự Kiosk.
 * Sử dụng Jetpack Paging 3 + cachedIn(viewModelScope) để:
 * 1. Phân trang theo nhu cầu từ Backend AI Server.
 * 2. Lưu cache trong vòng đời ViewModel, không load lại khi recompose / xoay màn hình.
 * 3. Tự động giải phóng (evict) các trang nằm ngoài tầm nhìn, ngăn ngừa phình to RAM.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class EnrollmentViewModel(
    private val listEmployeeUseCase: ListEmployeeUseCase = ListEmployeeUseCase(RepositoryModule.employeeRepository)
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedDepartment = MutableStateFlow("Tất cả")

    private val _uiState = MutableStateFlow(EnrollmentUiState())
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    // Tối ưu hóa: Tránh sử dụng Flow.map trung gian, lắng nghe trực tiếp luồng tìm kiếm và phòng ban
    val employeesPagingFlow: Flow<PagingData<Employee>> = combine(
        _searchQuery.debounce { if (it.isEmpty()) 0L else 300L },
        _selectedDepartment
    ) { query, dept ->
        Pair(query, dept)
    }
        .distinctUntilChanged()
        .flatMapLatest { (query, dept) ->
            listEmployeeUseCase(query = query, department = dept)
        }
        .cachedIn(viewModelScope)

    fun onEvent(event: EnrollmentUiEvent) {
        when (event) {
            is EnrollmentUiEvent.OnSearchQueryChanged -> {
                _searchQuery.value = event.query
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is EnrollmentUiEvent.OnSelectDepartment -> {
                _selectedDepartment.value = event.department
                _uiState.update { it.copy(selectedDepartment = event.department) }
            }
            is EnrollmentUiEvent.OnClearSearch -> {
                _searchQuery.value = ""
                _uiState.update { it.copy(searchQuery = "") }
            }
            is EnrollmentUiEvent.OnRefresh -> {
                // UI sẽ gọi trực tiếp lazyPagingItems.refresh()
            }
        }
    }
}
