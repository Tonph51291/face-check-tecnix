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
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.enrollment.AttendanceHistoryItem
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Thẻ Lịch sử chấm công gần nhất (Chuẩn giao diện Stitch, hỗ trợ nhận dữ liệu động từ API)
 */
@Composable
fun AttendanceHistoryCard(
    historyItems: List<AttendanceHistoryItem>? = null,
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
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = EnrollmentColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Lịch sử chấm công gần nhất",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EnrollmentColors.OnSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!historyItems.isNullOrEmpty()) {
                historyItems.take(5).forEach { item ->
                    AttendanceLogItem(
                        date = item.date,
                        time = item.time,
                        status = item.status,
                        isGood = item.isGood
                    )
                }
            } else {
                // Fallback nếu nhân viên chưa có lịch sử nào trên BE
                AttendanceLogItem("Hôm nay", "--:--", "Chưa chấm công", isGood = true)
                AttendanceLogItem("Hôm qua", "--:--", "Không có dữ liệu", isGood = true)
            }
        }
    }
}

@Composable
private fun AttendanceLogItem(
    date: String,
    time: String,
    status: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = date, fontSize = 12.5.sp, color = EnrollmentColors.OnSurface)
        Text(text = time, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = EnrollmentColors.OnSurface)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (isGood) Color(0xFFE8F8F0) else Color(0xFFFFDAD6))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = status,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGood) Color(0xFF005230) else Color(0xFF93000A)
            )
        }
    }
}
