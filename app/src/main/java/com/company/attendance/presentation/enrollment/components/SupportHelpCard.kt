package com.company.attendance.presentation.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Thẻ trợ giúp & Hỗ trợ HR khi không tìm thấy nhân viên
 */
@Composable
fun SupportHelpCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EnrollmentColors.SurfaceContainer)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EnrollmentColors.SurfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ContactSupport,
                    contentDescription = null,
                    tint = EnrollmentColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Không tìm thấy thông tin nhân viên?",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = EnrollmentColors.OnSurface
                )
                Text(
                    text = "Vui lòng kiểm tra lại chính tả hoặc liên hệ phòng Hành chính Nhân sự (HR) để tạo hồ sơ điện tử trước khi đăng ký nhận diện khuôn mặt.",
                    fontSize = 12.5.sp,
                    color = EnrollmentColors.OnSurfaceVariant,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = EnrollmentColors.Primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Hotline: 1080",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnrollmentColors.Primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = null,
                            tint = EnrollmentColors.Primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "hr@tecnix.vn",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EnrollmentColors.Primary
                        )
                    }
                }
            }
        }
    }
}
