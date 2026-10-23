package com.company.attendance.data.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.repository.EmployeeRepository

/**
 * PagingSource tải danh sách nhân viên theo từng trang từ Server.
 * Giúp tối ưu hóa bộ nhớ RAM và băng thông mạng.
 */
class EmployeePagingSource(
    private val employeeRepository: EmployeeRepository,
    private val query: String? = null,
    private val department: String? = null
) : PagingSource<Int, Employee>() {

    override fun getRefreshKey(state: PagingState<Int, Employee>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Employee> {
        val page = params.key ?: 1
        val limit = params.loadSize

        return when (val result = employeeRepository.getEmployeesPaged(
            query = query,
            department = department,
            page = page,
            limit = limit
        )) {
            is Result.Success -> {
                val employees = result.data
                val nextKey = if (employees.isEmpty() || employees.size < limit) null else page + 1
                val prevKey = if (page == 1) null else page - 1

                LoadResult.Page(
                    data = employees,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            }
            is Result.Error -> {
                LoadResult.Error(Exception(result.error.message))
            }
        }
    }
}
