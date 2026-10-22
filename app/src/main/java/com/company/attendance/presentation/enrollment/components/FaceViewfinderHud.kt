package com.company.attendance.presentation.enrollment.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Khung ngắm Camera HUD công nghệ cao (4 góc kim loại cyan, oval chấm bi, thước đo chất lượng)
 */
@Composable
fun FaceViewfinderHud(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "capture_scan")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .drawBehind {
                val cornerLength = 28.dp.toPx()
                val strokeWidth = 3.5.dp.toPx()
                val radius = 8.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val cornerColor = Color(0xFF67E8F9)

                // Top-Left
                val pathTL = Path().apply {
                    moveTo(16.dp.toPx(), 16.dp.toPx() + cornerLength)
                    lineTo(16.dp.toPx(), 16.dp.toPx() + radius)
                    quadraticTo(16.dp.toPx(), 16.dp.toPx(), 16.dp.toPx() + radius, 16.dp.toPx())
                    lineTo(16.dp.toPx() + cornerLength, 16.dp.toPx())
                }
                drawPath(pathTL, cornerColor, style = stroke)

                // Top-Right
                val pathTR = Path().apply {
                    moveTo(size.width - 16.dp.toPx() - cornerLength, 16.dp.toPx())
                    lineTo(size.width - 16.dp.toPx() - radius, 16.dp.toPx())
                    quadraticTo(size.width - 16.dp.toPx(), 16.dp.toPx(), size.width - 16.dp.toPx(), 16.dp.toPx() + radius)
                    lineTo(size.width - 16.dp.toPx(), 16.dp.toPx() + cornerLength)
                }
                drawPath(pathTR, cornerColor, style = stroke)

                // Bottom-Left
                val pathBL = Path().apply {
                    moveTo(16.dp.toPx(), size.height - 16.dp.toPx() - cornerLength)
                    lineTo(16.dp.toPx(), size.height - 16.dp.toPx() - radius)
                    quadraticTo(16.dp.toPx(), size.height - 16.dp.toPx(), 16.dp.toPx() + radius, size.height - 16.dp.toPx())
                    lineTo(16.dp.toPx() + cornerLength, size.height - 16.dp.toPx())
                }
                drawPath(pathBL, cornerColor, style = stroke)

                // Bottom-Right
                val pathBR = Path().apply {
                    moveTo(size.width - 16.dp.toPx() - cornerLength, size.height - 16.dp.toPx())
                    lineTo(size.width - 16.dp.toPx() - radius, size.height - 16.dp.toPx())
                    quadraticTo(size.width - 16.dp.toPx(), size.height - 16.dp.toPx(), size.width - 16.dp.toPx(), size.height - 16.dp.toPx() - radius)
                    lineTo(size.width - 16.dp.toPx(), size.height - 16.dp.toPx() - cornerLength)
                }
                drawPath(pathBR, cornerColor, style = stroke)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Chip chỉ số căn chỉnh trên đỉnh camera
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EnrollmentColors.TertiaryFixed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Khoảng cách hợp lệ (65 cm) • Độ sáng: Tốt",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // Khung Oval sinh trắc học trung tâm
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .aspectRatio(0.75f)
                    .drawBehind {
                        drawOval(
                            color = Color(0xFF38BDF8),
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = Color(0xFF67E8F9).copy(alpha = pulseAlpha),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Đưa khuôn mặt vào khung",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Nhìn thẳng, giữ khuôn mặt đủ sáng",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Thanh đo chất lượng căn chỉnh
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Chất lượng căn chỉnh",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "94%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnrollmentColors.TertiaryFixed
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF334155))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.94f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(999.dp))
                                .background(EnrollmentColors.TertiaryFixed)
                        )
                    }
                }
            }
        }
    }
}
