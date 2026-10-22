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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.FaceViewfinderHud
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Màn hình 4A: Đăng ký khuôn mặt - Chụp mẫu (Face Enrollment Capture - MVVM Refactored)
 * Node ID: 3bda7a5093d44ab3b6fa20c5d5b06c1e
 */
@Composable
fun FaceCaptureScreen(
    employee: Employee,
    onNavigateBack: () -> Unit,
    onCaptureFace: () -> Unit,
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
                subtitle = "Đăng ký sinh trắc học",
                badgeText = "Camera Mode",
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
                                    text = "Bước 1/2: Căn chỉnh và chụp mẫu nhận diện",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EnrollmentColors.OnSurfaceVariant
                                )
                            }

                            // Chip camera sẵn sàng
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFE8F8F0))
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
                                            .background(EnrollmentColors.Tertiary)
                                    )
                                    Text(
                                        text = "CAMERA SẴN SÀNG",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EnrollmentColors.Tertiary
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // KHUNG NGẮM CAMERA (Camera Viewfinder Stage)
                        FaceViewfinderHud()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Nút Chụp mẫu khuôn mặt & Hủy (Responsive Row)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onCaptureFace,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EnrollmentColors.PrimaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Chụp mẫu khuôn mặt",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                                    text = "Dữ liệu khuôn mặt được mã hóa vector 512-dim an toàn theo chuẩn bảo mật sinh trắc học TECNIX.",
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
