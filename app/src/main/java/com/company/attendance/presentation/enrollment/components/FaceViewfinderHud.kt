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
 * - Top HUD Guidance Banner: Đặt phía trên Oval, nền tối mờ chống chói, chữ to rõ (14-15sp) với icon trạng thái phân màu trực quan.
 * - Khung Oval trung tâm: Hoàn toàn trong suốt, KHÔNG có chữ/icon đè lên mặt người dùng; viền đổi màu theo trạng thái căn chỉnh (Vàng -> Xanh).
 * - Bottom HUD Quality Meter: Hiển thị tiến độ giữ yên thực tế (0% -> 100%), loại bỏ số % ảo.
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
    val ovalColor = when {
        isUploading -> Color(0xFF818CF8) // Indigo
        isAligned && stabilityProgress > 0.5f -> Color(0xFF10B981) // Emerald-500 sáng rõ
        isAligned -> Color(0xFF38BDF8) // Sky-400
        else -> Color(0xFFF59E0B) // Amber-500 nổi bật báo cần căn chỉnh
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

        // 2. Lớp phủ HUD
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD GUIDANCE BANNER: Đặt phía trên Oval, tương phản cao, chữ to rõ ràng
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE0F172A)) // slate-900 mờ 93% chống chói hoàn hảo
                    .border(
                        width = 1.dp,
                        color = if (isAligned) Color(0xFF10B981) else Color(0xFFF59E0B),
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
                                .background(if (isAligned) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isUploading -> "Đang gửi dữ liệu AI..."
                                isAligned -> "Khuôn mặt hợp lệ • Giữ yên"
                                else -> distanceText.ifBlank { "Chưa căn chuẩn vị trí" }
                            },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = guidanceMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAligned) Color(0xFFA7F3D0) else Color(0xFFFDE68A),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // CENTER AI OVAL GUIDE: Trong suốt hoàn toàn để người dùng thấy rõ mặt mình
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
                                pathEffect = if (isAligned) null else PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Chỉ hiển thị overlay khi đang upload mẫu, khi chụp bình thường để trống 100%
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
                                text = "Đang lưu mẫu khuôn mặt...",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
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
