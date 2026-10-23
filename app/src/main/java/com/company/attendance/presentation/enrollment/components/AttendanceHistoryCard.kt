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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.domain.model.AttendanceHistoryItem

/**
 * Thẻ Lịch sử chấm công gần nhất chuẩn Stitch:
 * - Bo góc 24dp (rounded-3xl), viền Slate-100, nền trắng.
 * - Icon lịch sử trong khối màu chàm Indigo (indigo-100/70).
 * - Danh sách bản ghi chấm công: Dot trạng thái (Emerald/Amber), thời gian dạng mono, pill tag Đúng giờ/Đi muộn.
 */
@Composable
fun AttendanceHistoryCard(
    historyItems: List<AttendanceHistoryItem>? = null,
    modifier: Modifier = Modifier
) {
    if (historyItems.isNullOrEmpty()) {
        return
    }

    val itemsToDisplay = historyItems.take(5)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x0F000000))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xFFF1F5F9), shape = RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xB3E0E7FF)), // indigo-100/70
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5), // indigo-600
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Lịch sử chấm công gần nhất",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Danh sách các lượt chấm công
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsToDisplay.forEach { item ->
                    AttendanceHistoryRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun AttendanceHistoryRow(item: AttendanceHistoryItem) {
    val isWarning = !item.isGood
    val rowBg = if (isWarning) Color(0x66FFFBEB) else Color(0xFFF8FAFC) // amber-50/40 vs slate-50
    val rowBorder = if (isWarning) Color(0x99FEF3C7) else Color(0xFFF1F5F9) // amber-100/60 vs slate-100
    val dotColor = if (isWarning) Color(0xFFF59E0B) else Color(0xFF10B981) // amber-500 vs emerald-500

    val tagBg = if (isWarning) Color(0xFFFFE4E6) else Color(0xFFD1FAE5) // rose-100 vs emerald-100
    val tagText = if (isWarning) Color(0xFFBE123C) else Color(0xFF047857) // rose-700 vs emerald-700
    val tagBorder = if (isWarning) Color(0xFFFECDD3) else Color(0xFFA7F3D0) // rose-200 vs emerald-200

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .border(width = 1.dp, color = rowBorder, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Status dot + Date & Kiosk
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Column {
                    Text(
                        text = item.date,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1E293B) // slate-800
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.kioskName,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B) // slate-500
                    )
                }
            }

            // Right: Time + Status Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = item.time,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1E293B)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(tagBg)
                        .border(width = 1.dp, color = tagBorder, shape = RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagText
                    )
                }
            }
        }
    }
}
