package com.company.attendance.presentation.enrollment

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.model.EmployeeDetail
import com.company.attendance.presentation.enrollment.components.AttendanceHistoryCard
import com.company.attendance.presentation.enrollment.components.BiometricStatusSpotlightCard
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.ProfileOverviewCard
import com.company.attendance.presentation.enrollment.components.ShiftPolicyCard
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Màn hình Chi tiết nhân sự Kiosk chuẩn Stitch:
 * - Tối ưu RAM: Sử dụng Single ViewModel + LRU Cache ở tầng Repository.
 * - Phản hồi 0ms: Nếu hồ sơ đã có trong LRU Cache, hiển thị tức thì không cần tải lại.
 * - Responsive: Tự động căn giữa cân đối trên màn hình lớn (Kiosk/Tablet) và full width trên Mobile.
 */
@Composable
fun EmployeeDetailScreen(
    employeeId: String,
    onNavigateBack: () -> Unit,
    onStartFaceCapture: (Employee) -> Unit,
    modifier: Modifier = Modifier,
    capturedBitmap: Bitmap? = null,
    forceRefresh: Boolean = false,
    viewModel: EmployeeDetailViewModel = viewModel(key = "EmployeeDetail_$employeeId")
) {
    // 1. Kích hoạt lấy dữ liệu (tự động dùng LRU Cache nếu có, ép làm mới nếu forceRefresh = true)
    LaunchedEffect(employeeId, forceRefresh) {
        viewModel.loadEmployeeDetail(employeeId, forceRefresh = forceRefresh)
    }

    // 2. Thu thập UI State
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 3. Ổn định callback
    val onNavigateBackStable = remember(onNavigateBack) {
        {
            viewModel.clear()
            onNavigateBack()
        }
    }
    val onStartFaceCaptureStable = remember(onStartFaceCapture) { onStartFaceCapture }

    EmployeeDetailContent(
        detail = uiState.detail,
        capturedBitmap = capturedBitmap,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onNavigateBack = onNavigateBackStable,
        onStartFaceCapture = onStartFaceCaptureStable,
        onRetry = { viewModel.refresh() },
        modifier = modifier
    )
}

/**
 * Stateless Content Composable:
 * Toàn bộ tham số đều là kiểu @Immutable hoặc stable lambda, tối ưu Smart Recomposition.
 */
@Composable
fun EmployeeDetailContent(
    detail: EmployeeDetail?,
    capturedBitmap: Bitmap? = null,
    isLoading: Boolean,
    errorMessage: String?,
    onNavigateBack: () -> Unit,
    onStartFaceCapture: (Employee) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val employee = detail?.employee
    val shift = detail?.shift
    val recentAttendance = detail?.recentAttendance ?: emptyList()

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
                onNavigateBack = onNavigateBack
            )

            // 2. NỘI DUNG CUỘN RESPONSIVE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Thông báo lỗi nếu gọi API thất bại
                    if (errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EnrollmentColors.ErrorContainer)
                                .border(1.dp, EnrollmentColors.WarningTagBorder, RoundedCornerShape(12.dp))
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
                                color = EnrollmentColors.OnErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Thử lại",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EnrollmentColors.PrimaryDark,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EnrollmentColors.PrimaryLight)
                                    .clickable { onRetry() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (employee == null && isLoading) {
                        // Trạng thái đang tải dữ liệu lần đầu
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
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Đang tải hồ sơ nhân sự từ máy chủ AI...",
                                    fontSize = 13.sp,
                                    color = EnrollmentColors.OnSurfaceVariant
                                )
                            }
                        }
                    } else if (employee != null) {
                        // 1. Thẻ thông tin nhân viên tổng quan (Ảnh chụp thực tế sau đăng ký nếu có)
                        ProfileOverviewCard(
                            employee = employee,
                            capturedBitmap = capturedBitmap
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Thẻ Trọng Tâm: Trạng thái & Quản lý mẫu khuôn mặt sinh trắc học
                        BiometricStatusSpotlightCard(
                            employee = employee,
                            onStartFaceCapture = onStartFaceCapture
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Thẻ Thông tin vị trí & Quy định ca làm việc
                        ShiftPolicyCard(
                            shiftName = shift?.name ?: "Ca tiêu chuẩn",
                            timeRange = shift?.timeRange ?: "08:30 - 17:30",
                            allowedLateMinutes = shift?.allowedLateMinutes ?: 15,
                            assignedKiosk = shift?.assignedKiosk ?: "Kiosk Cổng 1"
                        )

                        // 4. Lịch sử chấm công gần đây (Chỉ hiển thị khi nhân viên đã có dữ liệu chấm công)
                        if (recentAttendance.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            AttendanceHistoryCard(
                                historyItems = recentAttendance
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Home Bar Indicator (Chuẩn Stitch footer)
                        Box(
                            modifier = Modifier
                                .width(128.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFCBD5E1))
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
