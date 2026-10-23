package com.company.attendance.presentation.enrollment.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Thẻ Tổng quan hồ sơ nhân sự chuẩn Stitch (HeroProfileCard):
 * - Avatar: Bo góc 16dp với Gradient Blue-to-Indigo, chấm xanh trực tuyến (Live pulse dot).
 * - Tên & Huy hiệu trạng thái "Đang làm việc".
 * - Chip mã nhân viên & vị trí chức danh.
 * - 3 khối liên hệ nhanh: Email, Số điện thoại, Ngày gia nhập.
 */
@Composable
fun ProfileOverviewCard(
    employee: Employee,
    capturedBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val avatarGradient = Brush.linearGradient(
        listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF3730A3))
    )

    val initial = employee.avatarInitials.ifBlank {
        employee.name.trim().split(" ").lastOrNull()?.take(1)?.uppercase() ?: employee.name.take(1).uppercase()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x0A0F172A))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, EnrollmentColors.BorderLight, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Phần trên: Avatar & Thông tin chính
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar 72dp bo góc 16dp kèm chấm xanh trạng thái
                Box(modifier = Modifier.size(72.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(avatarGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap.asImageBitmap(),
                                contentDescription = "Ảnh khuôn mặt",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = initial,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    // Chấm xanh trạng thái trực tuyến
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                // Cột Tên, Mã, Chức danh & Phòng ban
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = employee.name,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnrollmentColors.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Huy hiệu trạng thái làm việc
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFFA7F3D0), CircleShape)
                                .padding(horizontal = 9.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = employee.status.ifBlank { "Đang làm việc" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Hàng mã nhân viên & chức danh
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEEF4FF))
                                .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Mã: ${employee.code}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = employee.position,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Phòng ban
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = employee.department,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Phần dưới: 3 Khối liên hệ & ngày gia nhập (Grid Metrics)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.Transparent,
                        shape = RoundedCornerShape(0.dp)
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricPill(
                    icon = Icons.Default.Mail,
                    iconBg = Color(0xFFDBEAFE).copy(alpha = 0.6f),
                    iconTint = Color(0xFF2563EB),
                    label = "EMAIL",
                    value = employee.email,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.Call,
                    iconBg = Color(0xFFE0E7FF).copy(alpha = 0.6f),
                    iconTint = Color(0xFF4F46E5),
                    label = "SỐ ĐIỆN THOẠI",
                    value = employee.phone,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.CalendarMonth,
                    iconBg = Color(0xFFCCFBF1).copy(alpha = 0.6f),
                    iconTint = Color(0xFF0D9488),
                    label = "GIA NHẬP",
                    value = employee.joinDate,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
