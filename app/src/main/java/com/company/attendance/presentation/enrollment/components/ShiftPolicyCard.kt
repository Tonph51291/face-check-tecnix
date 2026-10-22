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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Thẻ hiển thị quy định ca làm việc (Hỗ trợ cấu hình ca động từ API Backend)
 */
@Composable
fun ShiftPolicyCard(
    shiftName: String = "Ca tiêu chuẩn",
    timeRange: String = "08:30 – 17:30",
    allowedLateMinutes: Int = 15,
    assignedKiosk: String = "Kiosk Cổng Chính 1",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEDF0FF), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = EnrollmentColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Quy định ca làm việc",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EnrollmentColors.OnSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = shiftName, fontSize = 11.sp, color = EnrollmentColors.OnSurfaceVariant)
                    Text(text = timeRange, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = EnrollmentColors.OnSurface)
                }
                Column {
                    Text(text = "Thời gian cho phép muộn", fontSize = 11.sp, color = EnrollmentColors.OnSurfaceVariant)
                    Text(text = "Tối đa $allowedLateMinutes phút", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = EnrollmentColors.Tertiary)
                }
                Column {
                    Text(text = "Thiết bị gán", fontSize = 11.sp, color = EnrollmentColors.OnSurfaceVariant)
                    Text(text = assignedKiosk, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = EnrollmentColors.Primary)
                }
            }
        }
    }
}
