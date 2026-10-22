package com.company.attendance.di

import com.company.attendance.data.repository.AttendanceRepositoryImpl
import com.company.attendance.data.repository.EmployeeRepositoryImpl
import com.company.attendance.domain.repository.AttendanceRepository
import com.company.attendance.domain.repository.EmployeeRepository

/**
 * Module cung cấp các thể hiện Repository cho toàn hệ thống theo kiến trúc MVVM.
 */
object RepositoryModule {
    val employeeRepository: EmployeeRepository by lazy {
        EmployeeRepositoryImpl()
    }

    val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepositoryImpl()
    }
}
