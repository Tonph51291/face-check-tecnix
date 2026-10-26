package com.company.attendance.presentation.enrollment

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.common.components.CameraPreview
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.FaceViewfinderHud
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Màn hình Đăng ký khuôn mặt chuẩn Stitch:
 * - Giao diện tinh gọn, tập trung cao độ, chuẩn xác 100% với màn hình Stitch Design.
 * - Hiển thị trạng thái Camera Live, thẻ bước chụp 1 dòng duy nhất, thông tin nhân sự và khung ngắm AI đen chuẩn xác.
 * - Code clean, responsive cho cả Mobile và Kiosk Tablet.
 */
@Composable
fun FaceCaptureScreen(
    employee: Employee,
    onNavigateBack: () -> Unit,
    onCaptureFace: (Bitmap?, com.company.attendance.domain.model.FaceQualityMetrics) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FaceCaptureViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val targetEmployeeId = employee.id.ifBlank { employee.code }
    LaunchedEffect(targetEmployeeId) {
        viewModel.initEmployee(targetEmployeeId)
    }

    // Lắng nghe one-off event từ ViewModel để điều hướng độc lập với State UI
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FaceCaptureEffect.NavigateToReview -> {
                    onCaptureFace(effect.bitmap, effect.metrics)
                }
                FaceCaptureEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFFF8FAFC) // slate-50
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP APP BAR
            EnrollmentTopBar(
                title = "TECNIX KIOSK",
                subtitle = "Đăng ký sinh trắc học",
                onNavigateBack = {
                    viewModel.onCancelClicked()
                }
            )

            // 2. NỘI DUNG CHÍNH (Layout cố định, không cuộn - triệt tiêu hoàn toàn giật rung)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header gọn: Gom thông tin nhân sự & Bước chụp trên CÙNG 1 KHỐI TINH GỌN (Compact Bar)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompactStaffAndStepBar(
                        employee = employee,
                        stepTitle = uiState.stepTitle,
                        sampleCount = uiState.sampleCount,
                        isUploading = uiState.isUploading
                    )

                    // Thông báo lỗi nếu server hoặc camera trả về lỗi
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CaptureErrorBanner(
                            errorMessage = uiState.errorMessage ?: "",
                            onDismiss = { viewModel.dismissError() },
                            onClearAndRetry = { viewModel.clearOldSamplesAndRetry() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Khung Camera AI Viewfinder - Chiếm trung tâm màn hình, cố định vững chắc
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FaceViewfinderHud(
                        modifier = Modifier.fillMaxSize(),
                        guidanceMessage = uiState.guidanceText,
                        distanceText = uiState.distanceText,
                        stabilityProgress = uiState.stabilityProgress,
                        isAligned = uiState.isFaceAligned,
                        isUploading = uiState.isUploading,
                        cameraContent = {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                isFrontCamera = true,
                                onFaceDetected = { faceData ->
                                    viewModel.onFaceFrame(faceData)
                                },
                                onCaptureReady = { provider ->
                                    viewModel.setBitmapProvider(provider)
                                }
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. BOTTOM ACTIONS TOOLBAR (Chuẩn Stitch footer)
            CaptureBottomToolbar(
                onCancel = {
                    viewModel.onCancelClicked()
                }
            )
        }
    }
}

/**
 * Thanh tóm tắt thông tin nhân sự và bước chụp trên CÙNG 1 KHỐI TINH GỌN (Compact Bar):
 * - Chiều cao chỉ ~48-52dp, không chiếm không gian màn hình Kiosk.
 * - Hiển thị Tên nhân viên, Mã NV, Phòng ban và Badge bước chụp hiện tại kèm số mẫu đã lưu.
 * - Đảm bảo vừa vặn toàn bộ giao diện trong 1 màn hình duy nhất mà không cần scroll.
 */
@Composable
private fun CompactStaffAndStepBar(
    employee: Employee,
    stepTitle: String,
    sampleCount: Int,
    isUploading: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "camera_dot_transition")
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0x0A000000))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bên trái: Avatar Initial + Tên NV + Mã NV
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF6FF))
                        .border(width = 1.dp, color = Color(0xFFDBEAFE), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.avatarInitials.ifBlank { employee.name.take(1).uppercase() },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = employee.name,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFDBEAFE))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = employee.code,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                    Text(
                        text = employee.department.ifBlank { "Nhân sự" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Bên phải: Badge Bước chụp + Số mẫu
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isUploading) Color(0xFFEFF6FF) else Color(0xFFF1F5F9))
                    .border(
                        width = 1.dp,
                        color = if (isUploading) Color(0xFFBFDBFE) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUploading) Color(0xFF2563EB)
                                else Color(0xFF10B981).copy(alpha = pingAlpha)
                            )
                    )
                    Text(
                        text = stepTitle.ifBlank { "Bước ${sampleCount + 1}/3" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUploading) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                    )
                    Text(
                        text = "($sampleCount/3)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }
            }
        }
    }
}

/**
 * Hộp cảnh báo lỗi
 */
@Composable
private fun CaptureErrorBanner(
    errorMessage: String,
    onDismiss: () -> Unit,
    onClearAndRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF2F2))
            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = errorMessage,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF991B1B),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Đóng",
                tint = Color(0xFF991B1B),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onDismiss() }
            )
        }

        if (errorMessage.contains("xóa mẫu", ignoreCase = true)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bấm để xóa mẫu cũ & chụp lại từ đầu",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFEE2E2))
                    .clickable { onClearAndRetry() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

/**
 * Thanh hành động chân trang chuẩn Stitch:
 * - Nút Hủy đăng ký (Secondary Slate)
 * - Home indicator bar
 */
@Composable
private fun CaptureBottomToolbar(
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .shadow(elevation = 4.dp, spotColor = Color(0x0F000000))
            .border(width = 1.dp, color = Color(0xFFE2E8F0))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nút Hủy đăng ký (Secondary Slate)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9)) // slate-100
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hủy đăng ký",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155) // slate-700
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Home Indicator Bar
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    }
}
