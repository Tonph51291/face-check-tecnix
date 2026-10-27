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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WarningAmber
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

import androidx.compose.runtime.remember

/**
 * Thẻ nhân viên chuẩn Stitch (rounded-2xl, border-slate-200/70, shadow-card).
 * - Avatar: Bo góc 12dp, gradient chuyển màu hiện đại, chữ cái viết tắt.
 * - Mã NV: Chip blue-50 viền blue-100.
 * - Tag trạng thái: Emerald (Đã có mẫu mặt / is_face_enrolled == true) hoặc Rose (Chưa có mẫu mặt).
 * - Nút xem chi tiết: Bo tròn 36dp viền slate-200/50.
 */
@Composable
fun EmployeeListItemCard(
    employee: Employee,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientIndex = remember(employee.avatarColorIndex, employee.name) {
        (employee.avatarColorIndex.takeIf { it >= 0 }
            ?: kotlin.math.abs(employee.name.hashCode())) % EnrollmentColors.AvatarGradients.size
    }
    val avatarGradient = EnrollmentColors.AvatarGradients[gradientIndex]

    val initials = remember(employee.avatarInitials, employee.name) {
        employee.avatarInitials.ifBlank {
            val trimmed = employee.name.trim()
            val spaceIdx = trimmed.lastIndexOf(' ')
            if (spaceIdx > 0 && spaceIdx < trimmed.length - 1) {
                "${trimmed.first().uppercaseChar()}${trimmed[spaceIdx + 1].uppercaseChar()}"
            } else {
                trimmed.take(2).uppercase()
            }
        }
    }

    // Kiểm tra trạng thái đăng ký sinh trắc học dựa trên isFaceEnrolled từ API backend
    val isEnrolled = employee.isFaceEnrolled || employee.hasFaceSample

    val cardBorderColor = if (!isEnrolled) {
        EnrollmentColors.WarningTagBorder.copy(alpha = 0.6f)
    } else {
        EnrollmentColors.OutlineVariant.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x0A0F172A))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar chữ cái với gradient hiện đại (rounded-xl 12dp)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(avatarGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
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

                        // Chip mã nhân viên (bg-blue-50 text-brand-700 border-blue-100)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EnrollmentColors.PrimaryLight)
                                .border(1.dp, EnrollmentColors.PrimaryBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = employee.code,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EnrollmentColors.PrimaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = employee.department,
                        fontSize = 12.sp,
                        color = EnrollmentColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tag trạng thái mẫu khuôn mặt (sử dụng is_face_enrolled từ API)
                    if (isEnrolled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EnrollmentColors.SuccessTagBg)
                                .border(1.dp, EnrollmentColors.SuccessTagBorder, CircleShape)
                                .padding(horizontal = 9.dp, vertical = 2.5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EnrollmentColors.SuccessIcon,
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
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EnrollmentColors.WarningTagBg)
                                .border(1.dp, EnrollmentColors.WarningTagBorder, CircleShape)
                                .padding(horizontal = 9.dp, vertical = 2.5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = EnrollmentColors.WarningIcon,
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

            Spacer(modifier = Modifier.width(8.dp))

            // Nút mũi tên chuyển tiếp tròn 36dp
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EnrollmentColors.Surface)
                    .border(1.dp, EnrollmentColors.OutlineVariant.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Xem chi tiết",
                    tint = EnrollmentColors.Outline,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
