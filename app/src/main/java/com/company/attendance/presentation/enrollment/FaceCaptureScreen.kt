package com.company.attendance.presentation.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.common.components.CameraPreview
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.FaceViewfinderHud
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Màn hình 4A: Đăng ký khuôn mặt - Chụp mẫu (Face Enrollment Capture - Smart Auto-Capture & Multi-Shot MVVM)
 * Node ID: 3bda7a5093d44ab3b6fa20c5d5b06c1e
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

    // Khi đã hoàn tất toàn bộ 3 mẫu (hasFaceSample == true) -> tự động chuyển sang xem lại kèm ảnh thực tế
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
        color = EnrollmentColors.Surface
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

            // 2. BODY CONTENT
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                    ) {
                        // Tiêu đề & Trạng thái camera
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Đăng ký khuôn mặt",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EnrollmentColors.OnSurface
                                )
                                Text(
                                    text = "${uiState.stepTitle} (${uiState.sampleCount}/3 mẫu đã lưu)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EnrollmentColors.OnSurfaceVariant
                                )
                            }

                            // Chip camera sẵn sàng / Đang gửi
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (uiState.isUploading) Color(0xFFEFF6FF) else Color(0xFFE8F8F0))
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
                                            .background(if (uiState.isUploading) Color(0xFF2563EB) else EnrollmentColors.Tertiary)
                                    )
                                    Text(
                                        text = if (uiState.isUploading) "AI ĐANG XỬ LÝ" else "CAMERA SẴN SÀNG",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isUploading) Color(0xFF2563EB) else EnrollmentColors.Tertiary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Thẻ thông tin nhân viên đang đăng ký
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFEDF0FF), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(EnrollmentColors.SurfaceContainerLow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = employee.avatarInitials.ifBlank { employee.name.take(2).uppercase() },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EnrollmentColors.Primary
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = employee.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EnrollmentColors.OnSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(EnrollmentColors.SurfaceContainerHigh)
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = employee.code,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EnrollmentColors.Primary
                                                )
                                            }
                                        }

                                        Text(
                                            text = employee.department,
                                            fontSize = 12.sp,
                                            color = EnrollmentColors.OnSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = EnrollmentColors.Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Xác nhận đúng thông tin nhân sự trước khi chụp",
                                        fontSize = 11.5.sp,
                                        color = EnrollmentColors.OnSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Thông báo lỗi nếu server trả về lỗi
                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
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
                                        text = uiState.errorMessage ?: "",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF991B1B),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Đóng thông báo",
                                        tint = Color(0xFF991B1B),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.dismissError() }
                                    )
                                }

                                if (uiState.errorMessage?.contains("xóa mẫu", ignoreCase = true) == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Bấm để xóa mẫu cũ & chụp lại từ đầu",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEE2E2))
                                            .clickable { viewModel.clearOldSamplesAndRetry() }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }



                        Spacer(modifier = Modifier.height(14.dp))

                        // KHUNG NGẮM CAMERA THỰC TẾ VỚI HUD CÔNG NGHỆ CAO
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Nút Chụp mẫu khuôn mặt & Hủy (Responsive Row)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerCapture() },
                                enabled = !uiState.isUploading,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EnrollmentColors.PrimaryContainer)
                            ) {
                                if (uiState.isUploading) {
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Chụp mẫu khuôn mặt (${uiState.sampleCount}/3)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = EnrollmentColors.SurfaceContainer,
                                    contentColor = EnrollmentColors.OnSurface
                                )
                            ) {
                                Text(
                                    text = "Hủy đăng ký",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hướng dẫn bảo mật sinh trắc học
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EnrollmentColors.SurfaceContainerLow)
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EnrollmentColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Hệ thống tự động chụp khi bạn giữ yên khuôn mặt 1.2s. Cần đủ 3 mẫu để kích hoạt nhận diện.",
                                    fontSize = 11.5.sp,
                                    color = EnrollmentColors.OnSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
