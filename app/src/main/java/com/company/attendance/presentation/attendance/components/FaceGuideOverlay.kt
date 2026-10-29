package com.company.attendance.presentation.attendance.components

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.attendance.RecognitionStatus

/**
 * Lớp phủ hướng dẫn quét khuôn mặt sinh trắc học chuẩn Stitch Kiosk:
 * - 4 góc ngắm kim loại Zinc-400 tối giản, tinh xảo.
 * - Top HUD Guidance Banner to rõ ở phía trên Oval, tương phản cao, chống chói hoàn hảo.
 * - Khung Oval trung tâm trong suốt 100%, KHÔNG bị icon hay tia laser che mặt người dùng.
 * - Viền Oval tự động đổi màu thông minh theo trạng thái (Vàng -> Xanh Emerald khi chuẩn).
 * - Bottom Recognition Meter hiển thị thanh tiến độ nhận diện thật sự và chứng chỉ bảo mật.
 */
@Composable
fun FaceGuideOverlay(
    status: RecognitionStatus,
    confidenceText: String,
    estimatedDistance: String,
    guidanceMessage: String,
    stabilityProgress: Float,
    modifier: Modifier = Modifier
) {
    val showScanningGuidance = (status != RecognitionStatus.SUCCESS && status != RecognitionStatus.FAILURE)

    val transition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val isStableOrVerifying = status == RecognitionStatus.FACE_STABLE ||
            status == RecognitionStatus.VERIFYING ||
            status == RecognitionStatus.SUCCESS ||
            status == RecognitionStatus.COOLDOWN

    val isAligning = status == RecognitionStatus.ALIGNING || status == RecognitionStatus.FACE_DETECTED

    val ovalColor = when {
        isStableOrVerifying -> Color(0xFF10B981) // Emerald-500 sáng rõ
        isAligning -> Color(0xFFF59E0B) // Amber-500 nổi bật
        else -> Color(0xFF38BDF8) // Sky-400
    }

    Box(
        modifier = modifier
            .fillMaxSize()
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
        if (showScanningGuidance) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP HUD GUIDANCE BANNER: Đặt phía trên Oval, tương phản cao, dễ nhìn từ xa
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xEE0F172A)) // slate-900 mờ 93% chống chói
                        .border(
                            width = 1.dp,
                            color = ovalColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Status Dot / Spinner
                        if (status == RecognitionStatus.VERIFYING) {
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
                                    .alpha(if (isStableOrVerifying) 1f else pulseAlpha)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            val titleText = when {
                                status == RecognitionStatus.VERIFYING -> "Đang đối chiếu AI..."
                                status == RecognitionStatus.COOLDOWN -> "Đã ghi nhận chấm công"
                                isStableOrVerifying -> "Khuôn mặt hợp lệ • Giữ yên"
                                status == RecognitionStatus.ALIGNING -> "Đang căn chỉnh vị trí..."
                                status == RecognitionStatus.FACE_DETECTED -> "Đã phát hiện khuôn mặt"
                                else -> "Đang tìm khuôn mặt..."
                            }

                            Text(
                                text = titleText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            val subText = if (guidanceMessage.isNotBlank()) {
                                guidanceMessage
                            } else if (estimatedDistance != "---" && estimatedDistance.isNotBlank()) {
                                "Khoảng cách: $estimatedDistance • Nhìn thẳng"
                            } else {
                                "Nhìn thẳng vào tâm khung oval để chấm công"
                            }

                            Text(
                                text = subText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isStableOrVerifying) Color(0xFFA7F3D0) else Color(0xFFFDE68A),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // 2. CENTER AI OVAL GUIDE: Trong suốt hoàn toàn, chỉ hiển thị khi cần căn chỉnh
                if (status != RecognitionStatus.IDLE && status != RecognitionStatus.FACE_DETECTED) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.70f)
                            .aspectRatio(0.82f)
                            .clip(RoundedCornerShape(50))
                            .drawBehind {
                                drawOval(
                                    color = ovalColor,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        pathEffect = if (isStableOrVerifying) null else PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Không có bất kỳ icon nào che giữa mặt!
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.70f)
                            .aspectRatio(0.82f)
                    )
                }

                // 3. BOTTOM HUD PROGRESS & SECURITY METER: Hiển thị tiến trình nhận diện thật sự
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xEE0F172A))
                        .border(width = 1.dp, color = Color(0xFF334155), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    val realProgress = stabilityProgress.coerceIn(0f, 1f)
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
                                        .background(if (realProgress > 0f) Color(0xFF10B981) else Color(0xFF64748B))
                                )
                                Text(
                                    text = when {
                                        status == RecognitionStatus.VERIFYING -> "AI đang xác thực danh tính..."
                                        realProgress > 0f -> "Tiến độ nhận diện khuôn mặt"
                                        else -> "Đưa khuôn mặt vào khung oval"
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (realProgress > 0f) Color(0xFFF1F5F9) else Color(0xFF94A3B8)
                                )
                            }

                            Text(
                                text = if (realProgress > 0f) "$percentText%" else "AI Sẵn sàng",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (realProgress > 0f) Color(0xFF34D399) else Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
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

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dòng bảo mật On-Device
                        Text(
                            text = "Nhận diện AI On-Device • Offline • AES-256",
                            color = Color(0xFF64748B),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}