package com.company.attendance.data.repository

import com.company.attendance.core.result.AppError
import com.company.attendance.core.result.Result
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.repository.EmployeeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Triển khai EmployeeRepository quản lý kho dữ liệu nhân sự trên thiết bị Kiosk.
 */
class EmployeeRepositoryImpl : EmployeeRepository {

    // Danh sách nhân viên đồng bộ cục bộ
    private val employees = mutableListOf(
        Employee(
            id = "1",
            name = "Nguyễn Văn An",
            code = "NV001",
            department = "Phòng Kỹ thuật & Công nghệ",
            position = "Kỹ sư Trưởng AI",
            email = "an.nguyen@tecnix.vn",
            phone = "0912 345 678",
            joinDate = "15/03/2022",
            hasFaceSample = false,
            avatarInitials = "NA",
            avatarColorIndex = 0
        ),
        Employee(
            id = "2",
            name = "Trần Thị Thu Hà",
            code = "NV002",
            department = "Phòng Hành chính Nhân sự",
            position = "Chuyên viên Tuyển dụng",
            email = "ha.tran@tecnix.vn",
            phone = "0988 765 432",
            joinDate = "01/06/2023",
            hasFaceSample = false,
            avatarInitials = "TH",
            avatarColorIndex = 1
        ),
        Employee(
            id = "3",
            name = "Lê Hoàng Minh",
            code = "NV003",
            department = "Phòng Kinh doanh",
            position = "Trưởng phòng Kinh doanh",
            email = "minh.le@tecnix.vn",
            phone = "0903 112 233",
            joinDate = "10/11/2021",
            hasFaceSample = true,
            avatarInitials = "LM",
            avatarColorIndex = 2
        ),
        Employee(
            id = "4",
            name = "Phạm Ngọc Quang",
            code = "NV004",
            department = "Phòng Tài chính Kế toán",
            position = "Kế toán Tổng hợp",
            email = "quang.pham@tecnix.vn",
            phone = "0977 445 566",
            joinDate = "20/08/2022",
            hasFaceSample = false,
            avatarInitials = "PQ",
            avatarColorIndex = 3
        ),
        Employee(
            id = "5",
            name = "Vũ Thị Mai",
            code = "NV005",
            department = "Phòng Hành chính Nhân sự",
            position = "Trưởng phòng Nhân sự",
            email = "mai.vu@tecnix.vn",
            phone = "0934 889 900",
            joinDate = "05/01/2020",
            hasFaceSample = true,
            avatarInitials = "VM",
            avatarColorIndex = 1
        ),
        Employee(
            id = "6",
            name = "Đặng Hoàng Nam",
            code = "NV006",
            department = "Phòng Kỹ thuật & Công nghệ",
            position = "Kỹ sư DevOps",
            email = "nam.dang@tecnix.vn",
            phone = "0915 223 344",
            joinDate = "12/04/2023",
            hasFaceSample = true,
            avatarInitials = "HN",
            avatarColorIndex = 0
        ),
        Employee(
            id = "7",
            name = "Bùi Phương Thảo",
            code = "NV007",
            department = "Phòng Kinh doanh",
            position = "Chuyên viên Tư vấn",
            email = "thao.bui@tecnix.vn",
            phone = "0966 778 899",
            joinDate = "18/09/2023",
            hasFaceSample = false,
            avatarInitials = "PT",
            avatarColorIndex = 2
        ),
        Employee(
            id = "8",
            name = "Hoàng Đức Anh",
            code = "NV008",
            department = "Phòng Tài chính Kế toán",
            position = "Kiểm toán Nội bộ",
            email = "anh.hoang@tecnix.vn",
            phone = "0944 556 677",
            joinDate = "02/02/2021",
            hasFaceSample = true,
            avatarInitials = "DA",
            avatarColorIndex = 3
        )
    )

    override suspend fun getEmployees(): Result<List<Employee>> = withContext(Dispatchers.IO) {
        Result.Success(employees.toList())
    }

    override suspend fun searchEmployees(query: String, department: String?): Result<List<Employee>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val filtered = employees.filter { emp ->
            val matchQuery = cleanQuery.isEmpty() ||
                emp.name.contains(cleanQuery, ignoreCase = true) ||
                emp.code.contains(cleanQuery, ignoreCase = true) ||
                emp.department.contains(cleanQuery, ignoreCase = true)

            val matchDept = department == null || department == "Tất cả" ||
                emp.department.contains(department, ignoreCase = true)

            matchQuery && matchDept
        }
        Result.Success(filtered)
    }

    override suspend fun getEmployeeById(id: String): Result<Employee> = withContext(Dispatchers.IO) {
        val found = employees.find { it.id == id }
        if (found != null) {
            Result.Success(found)
        } else {
            Result.Error(AppError.EmployeeNotFound())
        }
    }

    override suspend fun updateFaceEnrollmentStatus(employeeId: String, hasFaceSample: Boolean): Result<Employee> = withContext(Dispatchers.IO) {
        val index = employees.indexOfFirst { it.id == employeeId }
        if (index != -1) {
            val updated = employees[index].copy(hasFaceSample = hasFaceSample)
            employees[index] = updated
            Result.Success(updated)
        } else {
            Result.Error(AppError.EmployeeNotFound())
        }
    }
}
