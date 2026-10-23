package com.company.attendance.domain.usecase

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.company.attendance.data.repository.paging.EmployeePagingSource
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case cung cấp Flow<PagingData<Employee>> kết nối trực tiếp với Backend FaceCheck AI Service.
 * Áp dụng Paging 3 để phân trang theo nhu cầu, tự động giải phóng (evict) các page xa khỏi RAM (maxSize = 60).
 */
class ListEmployeeUseCase(
    private val employeeRepository: EmployeeRepository
) {
    operator fun invoke(
        query: String = "",
        department: String? = null
    ): Flow<PagingData<Employee>> {
        val normalizedQuery = query.trim().ifBlank { null }
        val normalizedDept = if (department == "Tất cả" || department.isNullOrBlank()) null else department

        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
                maxSize = 60 // Tối ưu RAM: chỉ giữ tối đa 60 item trong bộ nhớ
            ),
            pagingSourceFactory = {
                EmployeePagingSource(
                    employeeRepository = employeeRepository,
                    query = normalizedQuery,
                    department = normalizedDept
                )
            }
        ).flow
    }
}
