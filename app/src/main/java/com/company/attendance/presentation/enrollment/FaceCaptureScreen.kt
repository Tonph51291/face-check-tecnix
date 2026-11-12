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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.model.FaceQualityMetrics
import com.company.attendance.presentation.common.components.CameraPreview
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.FaceViewfinderHud

/**
 * Màn hình Đăng ký khuôn mặt đa góc (FaceCaptureScreen) chuẩn On-Device AI:
 * - Tự động trích xuất Vector 512D từ TFLite trên thiết bị.
 * - Gửi tuần tự 3 mẫu (Chính diện kèm avatar, 2 góc nghiêng) lên Backend.
 * - Xử lý thông báo lỗi chi tiết (Trùng mặt 409, Tráo người 400).
 */
@Composable
fun FaceCaptureScreen(
    employee: Employee,
    onNavigateBack: () -> Unit,
    onCaptureFace: (Bitmap?, FaceQualityMetrics, String?) -> Unit,
    modifier: Modifier = Modifier,
    // Tối ưu hóa: Dùng chung một ViewModel instance để tránh rò rỉ RAM (leak 24MB FaceEmbedder model cho mỗi nhân viên)
    viewModel: FaceCaptureViewModel = viewModel(key = "FaceCaptureSession")
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val targetEmployeeId = employee.id.ifBlank { employee.code }
    LaunchedEffect(targetEmployeeId) {
        viewModel.initEmployee(targetEmployeeId)
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FaceCaptureEffect.NavigateToReview -> {
                    onCaptureFace(effect.bitmap, effect.metrics, effect.avatarUrl)
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
        color = Color(0xFFF8FAFC)
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

            // 2. NỘI DUNG CHÍNH
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header gọn: Thông tin nhân sự + Tiến độ 3 mẫu
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompactStaffAndStepBar(
                        employee = employee,
                        stepTitle = uiState.stepTitle,
                        currentStep = uiState.currentStep,
                        samplesSuccess = uiState.samplesSuccess,
                        isUploading = uiState.isUploading,
                        uploadStatus = uiState.uploadStatusMessage
                    )

                    // Thông báo lỗi nếu có
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CaptureErrorBanner(
                            errorMessage = uiState.errorMessage ?: "",
                            onDismiss = { viewModel.dismissError() },
                            onRetryUpload = { viewModel.retryBatchUpload() },
                            onClearAndRetry = { viewModel.clearOldSamplesAndRetry() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Khung Camera AI Viewfinder
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
                        guidanceState = uiState.guidanceState,
                        showLeftArrow = uiState.showLeftArrow,
                        showRightArrow = uiState.showRightArrow,
                        cameraContent = {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                isFrontCamera = false,
                                onFaceDetected = { faceData ->
                                    viewModel.onFaceFrame(faceData)
                                },
                                onCaptureReady = { provider ->
                                    viewModel.setBitmapProvider(provider)
                                }
                            )
                        }
                    )

                    // Loading overlay khi đang đẩy vector lên server
                    if (uiState.isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = uiState.uploadStatusMessage ?: "Đang đồng bộ vector...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. BOTTOM ACTIONS TOOLBAR
            CaptureBottomToolbar(
                onCancel = {
                    viewModel.onCancelClicked()
                }
            )
        }
    }

    // DIALOG LỖI 409: TRÙNG LẶP KHUÔN MẶT
    if (uiState.isConflictDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConflictDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Khuôn mặt đã tồn tại (Lỗi 409)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF991B1B)
                )
            },
            text = {
                Text(
                    text = uiState.conflictErrorMessage ?: "Khuôn mặt này đã được đăng ký cho nhân viên khác trong hệ thống.",
                    fontSize = 13.5.sp,
                    color = Color(0xFF334155),
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissConflictDialog()
                        viewModel.clearOldSamplesAndRetry()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Chụp lại", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissConflictDialog() }) {
                    Text("Đóng")
                }
            }
        )
    }

    // DIALOG LỖI 400: KHÔNG KHỚP MẪU TRƯỚC ĐÓ (TRÁO NGƯỜI)
    if (uiState.isMismatchDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMismatchDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Khuôn mặt không khớp (Lỗi 400)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFC2410C)
                )
            },
            text = {
                Text(
                    text = uiState.mismatchErrorMessage ?: "Khuôn mặt chụp không khớp với mẫu trước đó. Vui lòng đảm bảo cùng một người thực hiện chụp đủ 3 góc.",
                    fontSize = 13.5.sp,
                    color = Color(0xFF334155),
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissMismatchDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                ) {
                    Text("Thử lại góc này", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    viewModel.dismissMismatchDialog()
                    viewModel.clearOldSamplesAndRetry()
                }) {
                    Text("Bắt đầu lại từ Mẫu 1")
                }
            }
        )
    }
}

/**
 * Thanh tóm tắt thông tin nhân sự và tiến độ 3 mẫu khuôn mặt
 */
@Composable
private fun CompactStaffAndStepBar(
    employee: Employee,
    stepTitle: String,
    currentStep: Int,
    samplesSuccess: List<Boolean>,
    isUploading: Boolean,
    uploadStatus: String?
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

            // Bên phải: 3 Dots biểu diễn tiến trình 3 mẫu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (step in 1..3) {
                    val isDone = samplesSuccess.getOrElse(step - 1) { false }
                    val isCurrent = (currentStep == step)

                    val bgColor = when {
                        isDone -> Color(0xFF10B981)
                        isCurrent -> Color(0xFF2563EB)
                        else -> Color(0xFFE2E8F0)
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Text(
                                text = "$step",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
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
    onRetryUpload: () -> Unit,
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

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Thử gửi lại",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D4ED8),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFDBEAFE))
                    .clickable { onRetryUpload() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )

            Text(
                text = "Chụp lại từ đầu",
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
 * Thanh hành động chân trang
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hủy đăng ký",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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
