package com.company.attendance.presentation.attendance.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.common.theme.OnPrimary
import com.company.attendance.presentation.common.theme.OnSurfaceText
import com.company.attendance.presentation.common.theme.OnSurfaceVariant
import com.company.attendance.presentation.common.theme.PrimaryBlue
import com.company.attendance.presentation.common.theme.SuccessGreen
import com.company.attendance.presentation.common.theme.SurfaceContainer
import com.company.attendance.presentation.common.theme.SurfaceContainerLowest

/**
 * Thanh tiêu đề Kiosk (KioskHeader) chuẩn thiết kế Stitch
 */
@Composable
fun KioskHeader(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    resolutionStatusText: String = "1080p Online"
) {
    val transition = rememberInfiniteTransition(label = "pulse_green_dot")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo thương hiệu TECNIX
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Logo TECNIX",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TECNIX",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF1E3A8A)
                )
                Text(
                    text = "Kiosk Chấm công AI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF64748B)
                )
            }
        }

        // Trạng thái camera / AI & Nút cài đặt
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pill trạng thái AI/Camera chuẩn Stitch
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF))
                    .border(1.dp, Color(0xFFBFDBFE).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .alpha(dotAlpha)
                    )
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = resolutionStatusText,
                        color = Color(0xFF1E3A8A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Nút mở bảng Cài đặt / Mô phỏng Demo
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cài đặt hệ thống",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
