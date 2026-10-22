package com.company.attendance.presentation.attendance.components

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
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.common.theme.OnSurfaceText
import com.company.attendance.presentation.common.theme.OnSurfaceVariant
import com.company.attendance.presentation.common.theme.SuccessGreen
import com.company.attendance.presentation.common.theme.SurfaceContainer
import com.company.attendance.presentation.common.theme.SurfaceContainerLowest

/**
 * Thẻ thông tin chấm công (DigitalClockBar):
 * Bố cục 2 hàng rõ ràng, thời gian lớn nổi bật, nhãn "Chấm công tự động" nằm ngang không bị bẻ dọc.
 */
@Composable
fun DigitalClockBar(
    timeString: String,
    dateString: String,
    shiftName: String,
    modifier: Modifier = Modifier,
    statusText: String = "Chấm công tự động",
    isOnline: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Hàng 1: Thời gian lớn nổi bật bên trái, Ngày và Ca làm việc bên phải
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Giờ số lớn dễ đọc từ xa (font mono đậm nét chuẩn Stitch)
            Text(
                text = timeString,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 36.sp,
                    letterSpacing = (-1.0).sp
                ),
                color = Color(0xFF0F172A)
            )

            // Ngày tháng và ca làm việc căn phải
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = shiftName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.End
                )
            }
        }

        // Đường kẻ phân cách tinh tế chuẩn thiết kế Stitch
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFF1F5F9))
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Hàng 2: Trạng thái Chấm công tự động & Chỉ báo hoạt động (nằm ngang trọn vẹn)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge Chấm công tự động
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFECFDF5))
                    .border(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.8f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = statusText,
                        color = Color(0xFF065F46),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Chỉ báo trạng thái hoạt động
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF10B981) else Color(0xFF94A3B8))
                )
                Text(
                    text = if (isOnline) "Sẵn sàng nhận diện" else "Đang khởi động",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
