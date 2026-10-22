package com.company.attendance.presentation.enrollment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.components.AttendanceHistoryCard
import com.company.attendance.presentation.enrollment.components.BiometricStatusSpotlightCard
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.ProfileOverviewCard
import com.company.attendance.presentation.enrollment.components.ShiftPolicyCard
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Màn hình: Chi tiết nhân viên chuẩn Stitch (MVVM Refactored)
 * Node ID: 5333fc82baf84cbfba770a02d79fbc86
 */
@Composable
fun EmployeeDetailScreen(
    employee: Employee,
    onNavigateBack: () -> Unit,
    onStartFaceCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = EnrollmentColors.Surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP APP BAR
            EnrollmentTopBar(
                title = "TECNIX KIOSK",
                subtitle = "Hồ sơ nhân sự sinh trắc học",
                badgeText = "Employee Details",
                onNavigateBack = onNavigateBack
            )

            // 2. NỘI DUNG CUỘN THÔNG TIN CHI TIẾT
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // 1. Thẻ thông tin nhân viên tổng quan
                    ProfileOverviewCard(employee = employee)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Thẻ Trọng Tâm: Dữ liệu sinh trắc học khuôn mặt
                    BiometricStatusSpotlightCard(
                        employee = employee,
                        onStartFaceCapture = onStartFaceCapture
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Thẻ Thông tin vị trí & Quy định ca làm việc
                    ShiftPolicyCard()

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Lịch sử chấm công gần đây (3 ngày gần nhất)
                    AttendanceHistoryCard()

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
