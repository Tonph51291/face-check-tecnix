package com.company.attendance.presentation.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Thẻ Trọng Tâm: Trạng thái Dữ liệu sinh trắc học khuôn mặt
 */
@Composable
fun BiometricStatusSpotlightCard(
    employee: Employee,
    onStartFaceCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(
                1.dp,
                if (employee.hasFaceSample) Color(0xFFD1FAE5) else Color(0xFFFFE4E6),
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (employee.hasFaceSample) Color(0xFFE8F8F0) else EnrollmentColors.ErrorContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (employee.hasFaceSample) Icons.Default.CheckCircle else Icons.Default.Face,
                            contentDescription = null,
                            tint = if (employee.hasFaceSample) EnrollmentColors.Tertiary else EnrollmentColors.OnErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (employee.hasFaceSample) "Đã đăng ký sinh trắc học" else "Chưa có dữ liệu khuôn mặt",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnrollmentColors.OnSurface
                        )
                        Text(
                            text = if (employee.hasFaceSample) "Mẫu khuôn mặt AI đã lưu trữ & mã hóa" else "Yêu cầu đăng ký để sử dụng Kiosk chấm công",
                            fontSize = 11.5.sp,
                            color = EnrollmentColors.OnSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (employee.hasFaceSample) EnrollmentColors.SuccessTagBg else EnrollmentColors.WarningTagBg
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (employee.hasFaceSample) "AI Verified" else "Chưa đăng ký",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (employee.hasFaceSample) EnrollmentColors.SuccessTagText else EnrollmentColors.WarningTagText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!employee.hasFaceSample) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EnrollmentColors.SurfaceContainer)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Nhân viên chưa thể chấm công tự động bằng khuôn mặt. Vui lòng bấm nút bên dưới để tiến hành căn chỉnh và chụp mẫu nhận diện tại Kiosk.",
                            fontSize = 12.sp,
                            color = EnrollmentColors.OnSurfaceVariant,
                            lineHeight = 16.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onStartFaceCapture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EnrollmentColors.Primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Đăng ký khuôn mặt ngay bây giờ",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartFaceCapture,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = EnrollmentColors.SurfaceContainerLow,
                            contentColor = EnrollmentColors.Primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Chụp lại / Cập nhật mẫu",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
