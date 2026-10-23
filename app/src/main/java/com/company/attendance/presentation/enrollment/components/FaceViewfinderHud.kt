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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Khung ngắm Camera AI chuẩn Stitch (CameraViewfinder):
 * - Tỉ lệ khung 3:3.6 bo góc 16dp (rounded-2xl) với nền đen và viền zinc-800.
 * - 4 góc kim loại HUD tối giản zinc-400.
 * - Top HUD Guidance Chip dạng viên thuốc viền zinc-700/80 với dot chuyển trạng thái Amber/Emerald.
 * - Khung Oval dẫn hướng AI bo tròn giữa màn hình với hiệu ứng nhịp thở nhẹ nhàng.
 * - Thước đo chất lượng căn chỉnh đáy khung ngắm (Bottom HUD Quality Meter) chuẩn thanh trượt Brand-600.
 */
@Composable
fun FaceViewfinderHud(
    modifier: Modifier = Modifier,
    guidanceMessage: String = "Nhìn thẳng vào tâm ống kính",
    distanceText: String = "Chưa căn chuẩn vị trí",
    stabilityProgress: Float = 0f,
    isAligned: Boolean = false,
    isUploading: Boolean = false,
    cameraContent: (@Composable () -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_animation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val ovalColor = when {
        isUploading -> Color(0xFF818CF8) // Indigo
        isAligned && stabilityProgress > 0.5f -> Color(0xFF34D399) // Emerald-400
        isAligned -> Color(0xFF60A5FA) // Blue-400
        else -> Color(0xFFA1A1AA) // Zinc-400
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 3.6f)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(width = 1.dp, color = Color(0xFF27272A), shape = RoundedCornerShape(16.dp))
            .drawBehind {
                // HUD 4 Corner Precision Brackets (Zinc-400)
                val cornerLength = 20.dp.toPx()
                val strokeWidth = 2.dp.toPx()
                val radius = 4.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val cornerColor = Color(0xFFA1A1AA) // zinc-400

                // Top-Left
                val pathTL = Path().apply {
                    moveTo(14.dp.toPx(), 14.dp.toPx() + cornerLength)
                    lineTo(14.dp.toPx(), 14.dp.toPx() + radius)
                    quadraticTo(14.dp.toPx(), 14.dp.toPx(), 14.dp.toPx() + radius, 14.dp.toPx())
                    lineTo(14.dp.toPx() + cornerLength, 14.dp.toPx())
                }
                drawPath(pathTL, cornerColor, style = stroke)

                // Top-Right
                val pathTR = Path().apply {
                    moveTo(size.width - 14.dp.toPx() - cornerLength, 14.dp.toPx())
                    lineTo(size.width - 14.dp.toPx() - radius, 14.dp.toPx())
                    quadraticTo(size.width - 14.dp.toPx(), 14.dp.toPx(), size.width - 14.dp.toPx(), 14.dp.toPx() + radius)
                    lineTo(size.width - 14.dp.toPx(), 14.dp.toPx() + cornerLength)
                }
                drawPath(pathTR, cornerColor, style = stroke)

                // Bottom-Left
                val pathBL = Path().apply {
                    moveTo(14.dp.toPx(), size.height - 14.dp.toPx() - cornerLength)
                    lineTo(14.dp.toPx(), size.height - 14.dp.toPx() - radius)
                    quadraticTo(14.dp.toPx(), size.height - 14.dp.toPx(), 14.dp.toPx() + radius, size.height - 14.dp.toPx())
                    lineTo(14.dp.toPx() + cornerLength, size.height - 14.dp.toPx())
                }
                drawPath(pathBL, cornerColor, style = stroke)

                // Bottom-Right
                val pathBR = Path().apply {
                    moveTo(size.width - 14.dp.toPx() - cornerLength, size.height - 14.dp.toPx())
                    lineTo(size.width - 14.dp.toPx() - radius, size.height - 14.dp.toPx())
                    quadraticTo(size.width - 14.dp.toPx(), size.height - 14.dp.toPx(), size.width - 14.dp.toPx(), size.height - 14.dp.toPx() - radius)
                    lineTo(size.width - 14.dp.toPx(), size.height - 14.dp.toPx() - cornerLength)
                }
                drawPath(pathBR, cornerColor, style = stroke)
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Luồng Camera trực tiếp
        if (cameraContent != null) {
            cameraContent()
        }

        // 2. Nội dung HUD phủ trên Camera
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Guidance Chip (Đúng chuẩn Stitch)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xE618181B)) // zinc-900/90
                    .border(width = 1.dp, color = Color(0xCC3F3F46), shape = RoundedCornerShape(999.dp)) // zinc-700/80
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isAligned) Color(0xFF34D399) else Color(0xFFFBBF24)) // emerald vs amber
                    )
                    Text(
                        text = if (isAligned) "Khuôn mặt hợp lệ" else distanceText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD4D4D8) // zinc-300
                    )
                }
            }

            // Center AI Oval Guide with Minimal Silhouette Icon (Đúng chuẩn Stitch)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .aspectRatio(0.85f)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x4D18181B)) // zinc-900/30
                    .drawBehind {
                        drawOval(
                            color = ovalColor.copy(alpha = 0.85f),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            )
                        )
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.size(42.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Đang lưu mẫu...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Minimal Face Silhouette Icon
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF27272A)) // zinc-800
                                .border(width = 1.dp, color = Color(0xFF3F3F46), shape = CircleShape), // zinc-700
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFD4D4D8), // zinc-300
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isAligned) "Giữ yên khuôn mặt" else "Đưa khuôn mặt vào khung",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF4F4F5), // zinc-100
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = guidanceMessage,
                            fontSize = 11.sp,
                            color = Color(0xFFA1A1AA), // zinc-400
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Bottom HUD Quality Meter (Chất lượng căn chỉnh)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF18181B)) // zinc-900
                    .border(width = 1.dp, color = Color(0xFF27272A), shape = RoundedCornerShape(12.dp)) // zinc-800
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                val displayProgress = if (stabilityProgress > 0f) stabilityProgress else if (isAligned) 0.85f else 0.75f
                val percentText = (displayProgress * 100).toInt()

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB)) // brand-500
                            )
                            Text(
                                text = "Chất lượng căn chỉnh",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD4D4D8) // zinc-300
                            )
                        }
                        Text(
                            text = "$percentText%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE4E4E7) // zinc-200
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF27272A)) // zinc-800
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                                .fillMaxSize()
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF2563EB)) // brand-600
                        )
                    }
                }
            }
        }
    }
}
