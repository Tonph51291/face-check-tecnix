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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.company.attendance.presentation.common.theme.CyanAccent
import com.company.attendance.presentation.common.theme.ErrorRed
import com.company.attendance.presentation.common.theme.PrimaryBlue
import com.company.attendance.presentation.common.theme.Slate900
import com.company.attendance.presentation.common.theme.SuccessGreen
import com.company.attendance.presentation.common.theme.WarningAmber

/**
 * Lớp phủ hướng dẫn quét khuôn mặt sinh trắc học chuẩn thiết kế Stitch Kiosk:
 * - 4 góc ngắm căn chỉnh bo tròn mềm mại
 * - Khung Oval sinh trắc học với hiệu ứng quét Laser tia cyan
 * - Icon target Avatar trung tâm dạng kính mờ (Backdrop Blur)
 * - Chip trạng thái căn chỉnh nổi phía trên
 * - Hộp thông báo vị trí và chỉ dẫn trực quan ở phía dưới
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
    val infiniteTransition = rememberInfiniteTransition(label = "scan_transition")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_progress"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val showScanningGuidance = (status != RecognitionStatus.SUCCESS && status != RecognitionStatus.FAILURE)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showScanningGuidance) {
            // 1. 4 góc ngắm sinh trắc học bo góc mềm mại (Biometric 4-Corner Guides chuẩn Stitch)
            val cornerColor = when (status) {
                RecognitionStatus.ALIGNING -> Color(0xFFF59E0B) // Amber-500
                RecognitionStatus.FACE_STABLE, RecognitionStatus.VERIFYING -> Color(0xFF22D3EE) // Cyan-400
                else -> Color(0xFF3B82F6) // Blue-500
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .drawBehind {
                        val cornerLength = 32.dp.toPx()
                        val strokeWidth = 4.dp.toPx()
                        val radius = 12.dp.toPx()
                        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

                        // Top-Left corner
                        val pathTL = Path().apply {
                            moveTo(0f, cornerLength)
                            lineTo(0f, radius)
                            quadraticTo(0f, 0f, radius, 0f)
                            lineTo(cornerLength, 0f)
                        }
                        drawPath(pathTL, cornerColor, style = stroke)

                        // Top-Right corner
                        val pathTR = Path().apply {
                            moveTo(size.width - cornerLength, 0f)
                            lineTo(size.width - radius, 0f)
                            quadraticTo(size.width, 0f, size.width, radius)
                            lineTo(size.width, cornerLength)
                        }
                        drawPath(pathTR, cornerColor, style = stroke)

                        // Bottom-Left corner (cách đáy 52dp để không che hint box)
                        val bottomY = size.height - 48.dp.toPx()
                        val pathBL = Path().apply {
                            moveTo(0f, bottomY - cornerLength)
                            lineTo(0f, bottomY - radius)
                            quadraticTo(0f, bottomY, radius, bottomY)
                            lineTo(cornerLength, bottomY)
                        }
                        drawPath(pathBL, cornerColor, style = stroke)

                        // Bottom-Right corner
                        val pathBR = Path().apply {
                            moveTo(size.width - cornerLength, bottomY)
                            lineTo(size.width - radius, bottomY)
                            quadraticTo(size.width, bottomY, size.width, bottomY - radius)
                            lineTo(size.width, bottomY - cornerLength)
                        }
                        drawPath(pathBR, cornerColor, style = stroke)
                    }
            )

            // 2. Khung Oval sinh trắc học trung tâm (Central Biometric Oval Target)
            val ovalColor = when (status) {
                RecognitionStatus.SUCCESS -> Color(0xFF34D399) // Emerald-400
                RecognitionStatus.FAILURE -> Color(0xFFFB7185) // Rose-400
                RecognitionStatus.ALIGNING -> Color(0xFFFBBF24) // Amber-400
                RecognitionStatus.FACE_STABLE, RecognitionStatus.VERIFYING -> Color(0xFF22D3EE) // Cyan-400
                else -> Color(0xFF38BDF8).copy(alpha = 0.85f) // Cyan-300
            }

            val isDashed =
                (status == RecognitionStatus.IDLE || status == RecognitionStatus.ALIGNING || status == RecognitionStatus.FACE_DETECTED)
            val ovalStrokeWidth = if (isDashed) 2.5.dp else 3.5.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .widthIn(min = 200.dp, max = 265.dp)
                    .aspectRatio(0.72f)
                    .drawBehind {
                        // Viền phát sáng nhẹ bao quanh
                        if (status == RecognitionStatus.SUCCESS || status == RecognitionStatus.FACE_STABLE || status == RecognitionStatus.VERIFYING) {
                            drawOval(
                                color = ovalColor.copy(alpha = 0.25f),
                                style = Stroke(width = 12.dp.toPx())
                            )
                        }

                        // Viền chính của Oval
                        val stroke = Stroke(
                            width = ovalStrokeWidth.toPx(),
                            pathEffect = if (isDashed) PathEffect.dashPathEffect(
                                floatArrayOf(
                                    18f,
                                    14f
                                ), 0f
                            ) else null
                        )
                        drawOval(
                            color = ovalColor,
                            style = stroke
                        )

                        // Vòng cung giữ yên khuôn mặt (Stability Progress Arc)
                        if (stabilityProgress > 0f) {
                            val progressStroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            drawArc(
                                color = Color(0xFF22D3EE),
                                startAngle = -90f,
                                sweepAngle = 360f * stabilityProgress,
                                useCenter = false,
                                style = progressStroke
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Tia Laser quét lên xuống mượt mà (Vertical Sweep Laser)
                if (status == RecognitionStatus.IDLE || status == RecognitionStatus.FACE_DETECTED || status == RecognitionStatus.ALIGNING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val lineY = size.height * scanProgress
                                val lineWidth = size.width * 0.82f
                                val startX = (size.width - lineWidth) / 2f
                                val endX = startX + lineWidth

                                // Tia laser chính
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF38BDF8).copy(alpha = 0.35f),
                                            Color(0xFF38BDF8),
                                            Color(0xFF22D3EE),
                                            Color(0xFF38BDF8),
                                            Color(0xFF38BDF8).copy(alpha = 0.35f),
                                            Color.Transparent
                                        ),
                                        startX = startX,
                                        endX = endX
                                    ),
                                    topLeft = Offset(startX, lineY - 1.5.dp.toPx()),
                                    size = Size(lineWidth, 3.dp.toPx())
                                )

                                // Hào quang mờ quanh tia laser
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF38BDF8).copy(alpha = 0.15f),
                                            Color(0xFF38BDF8).copy(alpha = 0.35f),
                                            Color(0xFF38BDF8).copy(alpha = 0.15f),
                                            Color.Transparent
                                        ),
                                        startX = startX,
                                        endX = endX
                                    ),
                                    topLeft = Offset(startX, lineY - 6.dp.toPx()),
                                    size = Size(lineWidth, 12.dp.toPx())
                                )
                            }
                    )
                }

                // Biểu tượng tâm ngắm Avatar dạng kính mờ (Backdrop Blur Glass chuẩn Stitch)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f))
                        .border(1.dp, Color.White.copy(alpha = 0.40f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (status) {
                            RecognitionStatus.SUCCESS -> Icons.Default.DoneAll
                            RecognitionStatus.FAILURE -> Icons.Default.Warning
                            RecognitionStatus.VERIFYING -> Icons.Default.HourglassTop
                            else -> Icons.Default.Face
                        },
                        contentDescription = "Target Avatar",
                        tint = when (status) {
                            RecognitionStatus.SUCCESS -> Color(0xFF34D399)
                            RecognitionStatus.FAILURE -> Color(0xFFFB7185)
                            RecognitionStatus.FACE_STABLE, RecognitionStatus.VERIFYING -> Color(
                                0xFF22D3EE
                            )

                            else -> Color.White.copy(alpha = pulseAlpha)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 3. Chip ngang trạng thái căn chỉnh ở đỉnh trên (Top Alignment Status Pill)
            val pillBg = when (status) {
                RecognitionStatus.SUCCESS -> Color(0xFF064E3B).copy(alpha = 0.85f)
                RecognitionStatus.FAILURE -> Color(0xFF4C0519).copy(alpha = 0.85f)
                RecognitionStatus.ALIGNING -> Color(0xFF451A03).copy(alpha = 0.85f)
                RecognitionStatus.FACE_STABLE, RecognitionStatus.VERIFYING -> Color(0xFF083344).copy(
                    alpha = 0.85f
                )

                else -> Color(0xFF0F172A).copy(alpha = 0.85f)
            }

            val pillBorder = when (status) {
                RecognitionStatus.SUCCESS -> Color(0xFF34D399).copy(alpha = 0.5f)
                RecognitionStatus.FAILURE -> Color(0xFFFB7185).copy(alpha = 0.5f)
                RecognitionStatus.ALIGNING -> Color(0xFFFBBF24).copy(alpha = 0.5f)
                RecognitionStatus.FACE_STABLE, RecognitionStatus.VERIFYING -> Color(0xFF22D3EE).copy(
                    alpha = 0.5f
                )

                else -> Color(0xFF38BDF8).copy(alpha = 0.35f)
            }

            val pillTextColor = when (status) {
                RecognitionStatus.SUCCESS -> Color(0xFF6EE7B7)
                RecognitionStatus.FAILURE -> Color(0xFFFDA4AF)
                RecognitionStatus.ALIGNING -> Color(0xFFFDE68A)
                RecognitionStatus.FACE_STABLE, RecognitionStatus.VERIFYING -> Color(0xFF67E8F9)
                else -> Color(0xFFE2E8F0)
            }

            val pingDotColor = when (status) {
                RecognitionStatus.SUCCESS -> Color(0xFF10B981)
                RecognitionStatus.FAILURE -> Color(0xFFEF4444)
                RecognitionStatus.ALIGNING -> Color(0xFFF59E0B)
                RecognitionStatus.FACE_STABLE, RecognitionStatus.VERIFYING -> Color(0xFF06B6D4)
                else -> Color(0xFF38BDF8)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(pillBg)
                    .border(1.dp, pillBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(pingDotColor)
                            .alpha(pulseAlpha)
                    )

                    val statusLabel = when (status) {
                        RecognitionStatus.IDLE -> "Đang tìm khuôn mặt..."
                        RecognitionStatus.FACE_DETECTED -> "Đã phát hiện khuôn mặt"
                        RecognitionStatus.ALIGNING -> "Đang căn chỉnh vị trí..."
                        RecognitionStatus.FACE_STABLE -> "Giữ yên khuôn mặt..."
                        RecognitionStatus.VERIFYING -> "Đang đối chiếu AI..."
                        RecognitionStatus.SUCCESS -> "Đã xác thực danh tính 100%"
                        RecognitionStatus.FAILURE -> "Không tìm thấy thông tin nhân viên"
                        RecognitionStatus.COOLDOWN -> "Đã ghi nhận"
                        else -> confidenceText
                    }

                    Text(
                        text = statusLabel,
                        color = pillTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (estimatedDistance != "---" && estimatedDistance.isNotBlank()) {
                        Text(
                            text = "•",
                            color = pillTextColor.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = estimatedDistance,
                            color = pillTextColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. Hộp hướng dẫn vị trí & thông báo bảo mật ở đáy Camera (Position Hint Box & Security Notice)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hộp thông báo kính mờ (Backdrop Blur Glass Box chuẩn Stitch)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.90f))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Circle icon con mắt quét
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = guidanceMessage,
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (estimatedDistance != "---" && estimatedDistance.isNotBlank()) {
                                        "Khoảng cách hiện tại: $estimatedDistance • Nhìn thẳng"
                                    } else {
                                        "Vui lòng giữ khuôn mặt bên trong khung oval"
                                    },
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dòng thông báo công nghệ On-Device Offline AES-256
                    Text(
                        text = "Nhận diện AI On-Device • Offline • AES-256",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
        }
    }
}