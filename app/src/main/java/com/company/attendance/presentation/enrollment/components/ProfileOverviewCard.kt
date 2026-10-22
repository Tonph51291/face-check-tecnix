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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Thẻ Tổng quan hồ sơ nhân sự (Profile Overview Card)
 */
@Composable
fun ProfileOverviewCard(
    employee: Employee,
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
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEDF0FF), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar lớn
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.avatarInitials.ifBlank { employee.name.take(2).uppercase() },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = avatarTextColor,
                        letterSpacing = 1.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = employee.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnrollmentColors.OnSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(EnrollmentColors.TertiaryFixed)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = employee.status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EnrollmentColors.OnTertiaryFixed
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EnrollmentColors.SurfaceContainerHigh)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Mã: ${employee.code}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EnrollmentColors.Primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EnrollmentColors.SurfaceContainer)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = employee.position,
                                fontSize = 12.sp,
                                color = EnrollmentColors.OnSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Domain,
                            contentDescription = null,
                            tint = EnrollmentColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = employee.department,
                            fontSize = 12.5.sp,
                            color = EnrollmentColors.OnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dải thông tin liên hệ nhanh (3 ô Responsive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoMiniCard(
                    icon = Icons.Default.Mail,
                    label = "Email",
                    value = employee.email,
                    modifier = Modifier.weight(1.2f)
                )
                InfoMiniCard(
                    icon = Icons.Default.Call,
                    label = "Số điện thoại",
                    value = employee.phone,
                    modifier = Modifier.weight(1f)
                )
                InfoMiniCard(
                    icon = Icons.Default.CalendarMonth,
                    label = "Gia nhập",
                    value = employee.joinDate,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}

@Composable
private fun InfoMiniCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(EnrollmentColors.SurfaceContainerLow)
            .padding(10.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EnrollmentColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 10.5.sp, color = EnrollmentColors.OnSurfaceVariant)
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EnrollmentColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
