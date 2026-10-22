package com.company.attendance.presentation.enrollment

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Composable Screen hiển thị giao diện tìm kiếm nhân viên và đăng ký khuôn mặt mới
 * (Tương thích chuẩn Stitch Node 428e3c00c38249afb630dcee3c593605)
 */
@Composable
fun EnrollmentScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmployeeSearchScreen(
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
