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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Thẻ hiển thị từng nhân viên (Employee Item Card)
 */
@Composable
fun EmployeeListItemCard(
    employee: Employee,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarBg = when (employee.avatarColorIndex % 4) {
        0 -> Color(0xFF2457C5)
        1 -> Color(0xFF809FFE)
        2 -> Color(0xFF006D41)
        else -> Color(0xFF7C3AED)
    }

    val avatarTextColor = when (employee.avatarColorIndex % 4) {
        1 -> Color(0xFF001849)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar chữ viết tắt
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.avatarInitials.ifBlank { employee.name.take(2).uppercase() },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = avatarTextColor,
                        letterSpacing = 0.5.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = employee.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnrollmentColors.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Mã nhân viên
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
                        fontSize = 12.5.sp,
                        color = EnrollmentColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Tag trạng thái mẫu khuôn mặt
                    if (employee.hasFaceSample) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(EnrollmentColors.SuccessTagBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EnrollmentColors.SuccessTagText,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Đã có mẫu mặt",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EnrollmentColors.SuccessTagText
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(EnrollmentColors.WarningTagBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = EnrollmentColors.WarningTagText,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Chưa có mẫu mặt",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EnrollmentColors.WarningTagText
                            )
                        }
                    }
                }
            }

            // Nút mũi tên chuyển tiếp
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(EnrollmentColors.SurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Chọn",
                    tint = EnrollmentColors.Outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
