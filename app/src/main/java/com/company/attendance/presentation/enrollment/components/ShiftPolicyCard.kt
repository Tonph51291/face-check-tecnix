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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Thẻ hiển thị quy định ca làm việc chuẩn Stitch:
 * - Bo góc 24dp (rounded-3xl), viền Slate-100, nền trắng.
 * - Icon đồng hồ trong khối xanh pastel (blue-100/70).
 * - Lưới 3 cột chính sách: Khung giờ ca (font-mono), Cho phép muộn (emerald), Thiết bị gán (blue-700).
 */
@Composable
fun ShiftPolicyCard(
    shiftName: String = "Hành chính tiêu chuẩn",
    timeRange: String = "08:30 - 17:30",
    allowedLateMinutes: Int = 15,
    assignedKiosk: String = "Kiosk Cổng 1",
    modifier: Modifier = Modifier
) {
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
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xB3DBEAFE)), // blue-100/70
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF2563EB), // blue-600
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Quy định ca làm việc",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$shiftName • Thiết bị gán mặc định",
                fontSize = 12.sp,
                color = Color(0xFF64748B) // slate-500
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Policy Columns Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCCF8FAFC)) // slate-50/80
                    .border(width = 1.dp, color = Color(0xFFF1F5F9), shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Col 1: Khung giờ ca
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KHUNG GIỜ CA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeRange,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0F172A)
                        )
                    }

                    // Col 2: Cho phép muộn (viền ngăn 2 bên)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(width = 1.dp, color = Color(0x40CBD5E1), shape = RoundedCornerShape(0.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CHO PHÉP MUỘN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tối đa $allowedLateMinutes phút",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669), // emerald-600
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Col 3: Thiết bị gán
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "THIẾT BỊ GÁN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = assignedKiosk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8), // blue-700
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
