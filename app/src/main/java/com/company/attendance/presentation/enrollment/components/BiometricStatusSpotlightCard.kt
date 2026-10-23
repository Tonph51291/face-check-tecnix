package com.company.attendance.presentation.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.domain.model.Employee

/**
 * Thẻ Trọng Tâm: Trạng thái Dữ liệu sinh trắc học khuôn mặt
 * Chuẩn thiết kế Stitch cho cả 2 trạng thái:
 * 1. Đã đăng ký (hasFaceSample == true): Thẻ viền ngọc lục bảo Emerald, nút Chụp lại/Cập nhật mẫu
 * 2. Chưa đăng ký (hasFaceSample == false): Thẻ viền hồng Rose với dải chuyển sắc Rose-Amber, hộp hướng dẫn & nút CTA Đăng ký ngay
 */
@Composable
fun BiometricStatusSpotlightCard(
    employee: Employee,
    onStartFaceCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (employee.hasFaceSample) {
        EnrolledBiometricCard(
            onRecapture = onStartFaceCapture,
            modifier = modifier
        )
    } else {
        NotEnrolledBiometricCard(
            onStartEnroll = onStartFaceCapture,
            modifier = modifier
        )
    }
}

/**
 * Trạng thái 1: Đã đăng ký sinh trắc học (Emerald theme)
 */
@Composable
private fun EnrolledBiometricCard(
    onRecapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emeraldBorder = Color(0x6634D399) // border-emerald-400/40
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0x1A10B981), // emerald-50/20
            Color.White,
            Color.White
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x0F000000))
            .clip(RoundedCornerShape(24.dp))
            .background(cardGradient)
            .border(width = 1.5.dp, color = emeraldBorder, shape = RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Checkmark Icon Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFD1FAE5)) // emerald-100
                        .border(1.dp, Color(0x806EE7B7), RoundedCornerShape(16.dp)), // emerald-300/50
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Đã xác minh",
                        tint = Color(0xFF059669), // emerald-600
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Text Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Đã đăng ký sinh trắc học",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Mẫu khuôn mặt AI đã lưu trữ & mã hóa an toàn ",
                            fontSize = 12.sp,
                            color = Color(0xFF475569), // slate-600
                            lineHeight = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9)) // slate-100
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "(Vector 512-dim)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1E293B) // slate-800
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: Chụp lại / Cập nhật mẫu (Secondary outline style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x99EFF6FF)) // blue-50/60
                    .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(14.dp))
                    .clickable { onRecapture() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8), // blue-700
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Chụp lại / Cập nhật mẫu",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1D4ED8)
                    )
                }
            }
        }
    }
}

/**
 * Trạng thái 2: Chưa có dữ liệu khuôn mặt (Rose/Amber warning theme)
 */
@Composable
private fun NotEnrolledBiometricCard(
    onStartEnroll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topAccentBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFB7185), // rose-400
            Color(0xFFF43F5E), // rose-500
            Color(0xFFFBBF24)  // amber-400
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x0F000000))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xCCFECDD3), shape = RoundedCornerShape(24.dp)) // border-rose-200/80
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle Top Glow / Accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .background(topAccentBrush)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Warning Header
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Face Icon in pastel rounded container
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFF1F2)) // rose-50
                            .border(1.dp, Color(0xFFFFE4E6), RoundedCornerShape(16.dp)), // rose-100
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Chưa có mẫu khuôn mặt",
                            tint = Color(0xFFF43F5E), // rose-500
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Chưa có dữ liệu khuôn mặt",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Yêu cầu đăng ký để sử dụng Kiosk chấm công",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B) // slate-500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Instructional Guidance Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x99EFF6FF)) // blue-50/60
                        .border(1.dp, Color(0xB3DBEAFE), RoundedCornerShape(16.dp)) // blue-100/70
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B), // amber-500
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 1.dp)
                        )
                        Text(
                            text = "Nhân viên chưa thể chấm công tự động bằng khuôn mặt. Vui lòng bấm nút bên dưới để tiến hành căn chỉnh và chụp mẫu nhận diện tại Kiosk.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155), // slate-700
                            lineHeight = 17.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Call-To-Action Button (Solid Brand Blue with Shadow)
                Button(
                    onClick = onStartEnroll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x402563EB)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB), // brand-600
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đăng ký khuôn mặt ngay bây giờ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}
