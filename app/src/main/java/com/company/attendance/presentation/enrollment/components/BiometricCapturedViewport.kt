package com.company.attendance.presentation.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Khung hình ảnh mẫu đã chụp với overlay HUD sinh trắc học và thông tin nhân viên
 */
@Composable
fun BiometricCapturedViewport(
    employee: Employee,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEDF0FF), RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Khung ảnh khuôn mặt công nghệ cao
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .drawBehind {
                        // 4 Corner HUD Precision Brackets
                        val cornerLength = 22.dp.toPx()
                        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        val cornerColor = Color(0xFF38BDF8)

                        // Top-Left
                        val pathTL = Path().apply {
                            moveTo(12.dp.toPx(), 12.dp.toPx() + cornerLength)
                            lineTo(12.dp.toPx(), 12.dp.toPx())
                            lineTo(12.dp.toPx() + cornerLength, 12.dp.toPx())
                        }
                        drawPath(pathTL, cornerColor, style = stroke)

                        // Top-Right
                        val pathTR = Path().apply {
                            moveTo(size.width - 12.dp.toPx() - cornerLength, 12.dp.toPx())
                            lineTo(size.width - 12.dp.toPx(), 12.dp.toPx())
                            lineTo(size.width - 12.dp.toPx(), 12.dp.toPx() + cornerLength)
                        }
                        drawPath(pathTR, cornerColor, style = stroke)

                        // Bottom-Left
                        val pathBL = Path().apply {
                            moveTo(12.dp.toPx(), size.height - 12.dp.toPx() - cornerLength)
                            lineTo(12.dp.toPx(), size.height - 12.dp.toPx())
                            lineTo(12.dp.toPx() + cornerLength, size.height - 12.dp.toPx())
                        }
                        drawPath(pathBL, cornerColor, style = stroke)

                        // Bottom-Right
                        val pathBR = Path().apply {
                            moveTo(size.width - 12.dp.toPx() - cornerLength, size.height - 12.dp.toPx())
                            lineTo(size.width - 12.dp.toPx(), size.height - 12.dp.toPx())
                            lineTo(size.width - 12.dp.toPx(), size.height - 12.dp.toPx() - cornerLength)
                        }
                        drawPath(pathBR, cornerColor, style = stroke)

                        // Center Oval Tracking Outline
                        drawOval(
                            color = Color(0xFF22D3EE).copy(alpha = 0.5f),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color(0xFF67E8F9).copy(alpha = 0.85f),
                    modifier = Modifier.size(80.dp)
                )

                // Floating Metadata Pills ở chân ảnh
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.80f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EnrollmentColors.TertiaryFixed)
                            )
                            Text(
                                text = "512-Vector Khớp",
                                fontSize = 10.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.80f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "AES-256 Mã hóa",
                            fontSize = 10.5.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Thanh danh tính nhân viên
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EnrollmentColors.SurfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = employee.avatarInitials.ifBlank { employee.name.take(2).uppercase() },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnrollmentColors.Primary
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = employee.name,
                                fontSize = 15.sp,
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

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EnrollmentColors.SurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = EnrollmentColors.Tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
