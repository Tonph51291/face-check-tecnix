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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
    onCaptureFace: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FaceCaptureViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val targetEmployeeId = employee.id.ifBlank { employee.code }
    LaunchedEffect(targetEmployeeId) {
        viewModel.initEmployee(targetEmployeeId)
    }

    // Khi đã hoàn tất toàn bộ 3 mẫu -> tự động chuyển sang màn hình Review
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onCaptureFace(uiState.lastCapturedBitmap)
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
                onNavigateBack = onNavigateBack
            )

            // 2. NỘI DUNG CUỘN CHÍNH
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
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tiêu đề màn hình & Camera Status Pill
                    CaptureHeaderStatusRow(isUploading = uiState.isUploading)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step Info Row: Bước chụp trên 1 dòng duy nhất (Chuẩn Stitch)
                    StepInfoRow(
                        stepTitle = uiState.stepTitle,
                        sampleCount = uiState.sampleCount
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Staff Card: Thẻ thông tin nhân sự đang đăng ký (Chuẩn Stitch)
                    StaffIdentityCard(employee = employee)

                    // Thông báo lỗi nếu server hoặc camera trả về lỗi
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CaptureErrorBanner(
                            errorMessage = uiState.errorMessage ?: "",
                            onDismiss = { viewModel.dismissError() },
                            onClearAndRetry = { viewModel.clearOldSamplesAndRetry() }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Khung Camera AI Viewfinder
                    FaceViewfinderHud(
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

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // 3. BOTTOM ACTIONS TOOLBAR (Chuẩn Stitch footer)
            CaptureBottomToolbar(
                sampleCount = uiState.sampleCount,
                isUploading = uiState.isUploading,
                onCapture = { viewModel.triggerCapture() },
                onCancel = onNavigateBack
            )
        }
    }
}

/**
 * Tiêu đề màn hình & Camera Status Pill (Hiệu ứng chấm xanh nhấp nháy)
 */
@Composable
private fun CaptureHeaderStatusRow(isUploading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "ping_transition")
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ping_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Đăng ký khuôn mặt",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A) // slate-900
        )

        // Camera Status Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (isUploading) Color(0xFFEFF6FF) else Color(0xFFECFDF5)) // emerald-50
                .border(
                    width = 1.dp,
                    color = if (isUploading) Color(0xFFBFDBFE) else Color(0xFFA7F3D0), // emerald-200
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUploading) Color(0xFF2563EB)
                            else Color(0xFF10B981).copy(alpha = pingAlpha)
                        )
                )
                Text(
                    text = if (isUploading) "AI ĐANG XỬ LÝ" else "CAMERA SẴN SÀNG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (isUploading) Color(0xFF1D4ED8) else Color(0xFF047857)
                )
            }
        }
    }
}

/**
 * Thẻ thông tin bước chụp trên CÙNG 1 DÒNG DUY NHẤT (Chuẩn Stitch step-info-card)
 */
@Composable
private fun StepInfoRow(stepTitle: String, sampleCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x0A000000))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB)) // brand-600
                )
                Text(
                    text = stepTitle.ifBlank { "Bước ${sampleCount + 1}/3: Chụp chính diện" },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B) // slate-800
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9)) // slate-100
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "($sampleCount/3 mẫu đã lưu)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B) // slate-500
                )
            }
        }
    }
}

/**
 * Thẻ thông tin nhân viên (Chuẩn Stitch user-info-card)
 */
@Composable
private fun StaffIdentityCard(employee: Employee) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x0A000000))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Badge Initial
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEFF6FF)) // brand-50
                        .border(width = 1.dp, color = Color(0xFFDBEAFE), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.avatarInitials.ifBlank { employee.name.take(1).uppercase() },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB) // brand-600
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = employee.name,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDBEAFE)) // brand-100
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = employee.code,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8) // brand-700
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = employee.department,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B) // slate-500
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ghi chú xác nhận dưới cùng
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Xác nhận đúng thông tin nhân sự trước khi chụp",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
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
 * Thanh nút bấm hành động chân trang chuẩn Stitch:
 * - Nút Chụp mẫu khuôn mặt (Primary Brand Blue)
 * - Nút Hủy đăng ký (Secondary Slate)
 * - Home indicator bar
 */
@Composable
private fun CaptureBottomToolbar(
    sampleCount: Int,
    isUploading: Boolean,
    onCapture: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút chụp chính (Primary brand-600)
                Button(
                    onClick = onCapture,
                    enabled = !isUploading,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB), // brand-600
                        contentColor = Color.White
                    )
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đang lưu mẫu...",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chụp mẫu khuôn mặt (${sampleCount}/3)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Nút Hủy đăng ký (Secondary Slate)
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9)) // slate-100
                        .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                        .clickable { onCancel() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hủy đăng ký",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155) // slate-700
                    )
                }
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
