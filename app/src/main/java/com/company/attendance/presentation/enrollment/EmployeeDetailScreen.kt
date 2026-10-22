package com.company.attendance.presentation.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.components.AttendanceHistoryCard
import com.company.attendance.presentation.enrollment.components.BiometricStatusSpotlightCard
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.ProfileOverviewCard
import com.company.attendance.presentation.enrollment.components.ShiftPolicyCard
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Màn hình: Chi tiết nhân viên chuẩn Stitch (MVVM Refactored & Recomposition Optimized)
 * Node ID: 5333fc82baf84cbfba770a02d79fbc86
 *
 * Chỉ nhận mã định danh `employeeId: String` (rất nhẹ, vài bytes), tự động gọi API lấy dữ liệu chi tiết.
 * Giữ nguyên 100% thiết kế giao diện, layout và màu sắc.
 */
@Composable
fun EmployeeDetailScreen(
    employeeId: String,
    onNavigateBack: () -> Unit,
    onStartFaceCapture: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmployeeDetailViewModel = viewModel(key = employeeId)
) {
    // 1. Kích hoạt gọi API bằng ID (chỉ tải khi ID thay đổi)
    LaunchedEffect(employeeId) {
        viewModel.loadEmployeeDetail(employeeId)
    }

    // 2. Thu thập UI State
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 3. Ổn định lambda callbacks
    val onNavigateBackStable = remember(onNavigateBack) { onNavigateBack }
    val onStartFaceCaptureStable = remember(onStartFaceCapture) { onStartFaceCapture }

    EmployeeDetailContent(
        employee = uiState.employee,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        shiftName = uiState.shiftName,
        shiftTimeRange = uiState.shiftTimeRange,
        allowedLateMinutes = uiState.allowedLateMinutes,
        recentAttendance = uiState.recentAttendance,
        onNavigateBack = onNavigateBackStable,
        onStartFaceCapture = onStartFaceCaptureStable,
        onRetry = { viewModel.refresh() },
        modifier = modifier
    )
}

/**
 * Stateless Content Composable:
 * Toàn bộ tham số đều là kiểu dữ liệu @Immutable, Primitive hoặc Stable Lambda.
 * Compose Compiler tự động bỏ qua Re-render nếu dữ liệu không thay đổi.
 */
@Composable
fun EmployeeDetailContent(
    employee: Employee?,
    isLoading: Boolean,
    errorMessage: String?,
    shiftName: String,
    shiftTimeRange: String,
    allowedLateMinutes: Int,
    recentAttendance: List<AttendanceHistoryItem>,
    onNavigateBack: () -> Unit,
    onStartFaceCapture: () -> Unit,
    onRetry: () -> Unit,
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
            // 1. TOP APP BAR (Giữ nguyên 100% giao diện)
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
                    // Thông báo lỗi nếu gọi API thất bại
                    if (errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EnrollmentColors.Error.copy(alpha = 0.12f))
                                .border(1.dp, EnrollmentColors.Error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = EnrollmentColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                fontSize = 12.sp,
                                color = EnrollmentColors.Error,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Thử lại",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EnrollmentColors.Primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EnrollmentColors.Primary.copy(alpha = 0.15f))
                                    .clickable { onRetry() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (employee == null && isLoading) {
                        // Trạng thái đang tải dữ liệu từ Backend
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = EnrollmentColors.Primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Đang tải hồ sơ nhân sự từ máy chủ AI...",
                                    fontSize = 13.sp,
                                    color = EnrollmentColors.OnSurfaceVariant
                                )
                            }
                        }
                    } else if (employee != null) {
                        // 1. Thẻ thông tin nhân viên tổng quan
                        ProfileOverviewCard(employee = employee)

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Thẻ Trọng Tâm: Dữ liệu sinh trắc học khuôn mặt
                        BiometricStatusSpotlightCard(
                            employee = employee,
                            onStartFaceCapture = onStartFaceCapture
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Thẻ Thông tin vị trí & Quy định ca làm việc (Cập nhật từ API)
                        ShiftPolicyCard(
                            shiftName = shiftName,
                            timeRange = shiftTimeRange,
                            allowedLateMinutes = allowedLateMinutes
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Lịch sử chấm công gần đây (Cập nhật từ API)
                        AttendanceHistoryCard(
                            historyItems = recentAttendance
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
