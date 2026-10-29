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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.enrollment.TurnGuidanceState

/**
 * Khung ngắm Camera AI chuẩn Stitch (CameraViewfinder):
 * - Tỉ lệ khung 3:3.6 bo góc 16dp với nền đen và viền zinc-800.
 * - Top HUD Guidance Banner: Đặt phía trên Oval, nền tối mờ chống chói, chữ to rõ (14-15sp) với icon trạng thái phân màu trực quan.
 * - Khung Oval trung tâm: Hoàn toàn trong suốt, viền đổi màu theo trạng thái (Vàng: chờ xoay, Đỏ: quá đà/nghiêng, Xanh: góc chuẩn tích lũy).
 * - Mũi tên chỉ hướng động: Xuất hiện nhấp nháy bên trái/phải khung oval khi cần người dùng quay đầu.
 * - Bottom HUD Quality Meter: Hiển thị tiến độ giữ yên thực tế (0% -> 100%).
 */
@Composable
fun FaceViewfinderHud(
    modifier: Modifier = Modifier,
    guidanceMessage: String = "Nhìn thẳng vào tâm ống kính",
    distanceText: String = "Chưa căn chuẩn vị trí",
    stabilityProgress: Float = 0f,
    isAligned: Boolean = false,
    isUploading: Boolean = false,
    guidanceState: TurnGuidanceState = TurnGuidanceState.WAITING_TURN,
    showLeftArrow: Boolean = false,
    showRightArrow: Boolean = false,
    cameraContent: (@Composable () -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_anim_transition")
    val arrowFloatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_float"
    )

    val ovalColor = when {
        isUploading -> Color(0xFF818CF8) // Indigo
        guidanceState == TurnGuidanceState.ALIGNED_HOLDING -> Color(0xFF10B981) // Emerald-500
        guidanceState == TurnGuidanceState.TURN_TOO_FAR || guidanceState == TurnGuidanceState.HEAD_TILTED -> Color(0xFFEF4444) // Red-500
        else -> Color(0xFFF59E0B) // Amber-500
    }

    Box(
        modifier = modifier
            .aspectRatio(3f / 3.6f, matchHeightConstraintsFirst = true)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(width = 1.dp, color = Color(0xFF27272A), shape = RoundedCornerShape(16.dp))
            .drawBehind {
                // HUD 4 Corner Precision Brackets (Zinc-400)
                val cornerLength = 22.dp.toPx()
                val strokeWidth = 2.5.dp.toPx()
                val radius = 4.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val cornerColor = Color(0xFFA1A1AA)

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

        // 2. Lớp phủ HUD
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD GUIDANCE BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE0F172A))
                    .border(
                        width = 1.dp,
                        color = ovalColor.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ovalColor)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isUploading -> "Đang lưu mẫu khuôn mặt..."
                                guidanceState == TurnGuidanceState.ALIGNED_HOLDING -> "Góc chuẩn xác • Giữ yên"
                                guidanceState == TurnGuidanceState.TURN_TOO_FAR -> "Quay quá đà • Cần chỉnh lại"
                                guidanceState == TurnGuidanceState.HEAD_TILTED -> "Đầu chưa thẳng"
                                else -> distanceText.ifBlank { "Chờ căn chỉnh vị trí" }
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = guidanceMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (guidanceState) {
                                TurnGuidanceState.ALIGNED_HOLDING -> Color(0xFFA7F3D0)
                                TurnGuidanceState.TURN_TOO_FAR, TurnGuidanceState.HEAD_TILTED -> Color(0xFFFCA5A5)
                                else -> Color(0xFFFDE68A)
                            },
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // CENTER AI OVAL GUIDE & DIRECTION ARROWS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Khung Oval
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .aspectRatio(0.82f)
                        .clip(RoundedCornerShape(50))
                        .drawBehind {
                            drawOval(
                                color = ovalColor,
                                style = if (guidanceState == TurnGuidanceState.ALIGNED_HOLDING) {
                                    Stroke(width = 3.5.dp.toPx())
                                } else {
                                    Stroke(
                                        width = 3.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                    )
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x990F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF60A5FA),
                                    modifier = Modifier.size(44.dp),
                                    strokeWidth = 3.5.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Đang lưu mẫu...",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // MŨI TÊN CHỈ HƯỚNG QUAY TRÁI (Bên trái oval)
                if (showLeftArrow && !isUploading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                            .graphicsLayer {
                                translationX = -arrowFloatOffset
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xD90F172A))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay Trái",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "QUAY TRÁI",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }

                // MŨI TÊN CHỈ HƯỚNG QUAY PHẢI (Bên phải oval)
                if (showRightArrow && !isUploading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .graphicsLayer {
                                translationX = arrowFloatOffset
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xD90F172A))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "QUAY PHẢI",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE68A)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Quay Phải",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // BOTTOM HUD QUALITY / STABILITY METER: Hiển thị tiến độ thật sự (0% -> 100%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE0F172A))
                    .border(width = 1.dp, color = Color(0xFF334155), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                val realProgress = if (isAligned) stabilityProgress.coerceIn(0f, 1f) else 0f
                val percentText = (realProgress * 100).toInt()

                Column {
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
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isAligned) Color(0xFF10B981) else Color(0xFF64748B))
                            )
                            Text(
                                text = when {
                                    isUploading -> "AI đang xử lý mẫu ảnh..."
                                    isAligned -> "Tiến độ quét khuôn mặt"
                                    else -> "Đưa khuôn mặt vào khung oval"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAligned) Color(0xFFF1F5F9) else Color(0xFF94A3B8)
                            )
                        }

                        Text(
                            text = if (isAligned) "$percentText%" else "Chờ căn chỉnh",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAligned) Color(0xFF34D399) else Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        if (realProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(realProgress)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }
                }
            }
        }
    }
}
