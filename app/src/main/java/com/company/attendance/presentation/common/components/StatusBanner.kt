package com.company.attendance.presentation.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.attendance.AttendanceResultData

import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ExitToApp

/**
 * Dialog thông báo Thành công (Floating Glass Modal chuẩn Stitch Node 069c4a349ffa4ee19807ad28db18f844)
 * - Tự động thích ứng màu sắc và thông điệp cho VÀO CA (Check-In) vs TAN CA (Check-Out)
 * - Tự động đếm ngược 3s & Countdown Progress Bar bo góc gọn gàng không bị lòi viền
 */
@Composable
fun SuccessStatusBanner(
    data: AttendanceResultData,
    remainingSeconds: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCheckOut = data.attendanceType.equals("CHECK_OUT", ignoreCase = true)
    val primaryColor = if (isCheckOut) Color(0xFF0284C7) else Color(0xFF10B981)
    val lightBgColor = if (isCheckOut) Color(0xFFE0F2FE) else Color(0xFFD1FAE5)
    val actionText = if (isCheckOut) "Tan ca" else "Vào ca"
    val subtitleText = if (isCheckOut) "Chấm công tan ca thành công" else "Chấm công vào ca thành công"

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color.Black.copy(alpha = 0.22f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(28.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Gradient chuyển tiếp trắng
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isCheckOut) {
                                listOf(Color(0xFFE0F2FE), Color(0xFFF0F9FF), Color.White)
                            } else {
                                listOf(Color(0xFFE8F8F0), Color(0xFFF2FBF6), Color.White)
                            }
                        )
                    )
                    .padding(top = 22.dp, bottom = 8.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Vòng tròn icon nổi bật viền 2.5dp
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCheckOut) Icons.AutoMirrored.Filled.ExitToApp else Icons.Default.Check,
                        contentDescription = "Thành công",
                        tint = primaryColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isCheckOut) "Hẹn Gặp Lại!" else "Thành công!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitleText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
            }

            // Body: Thông tin nhân viên & Nút tiếp tục
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tên nhân viên & Mã nhân viên căn giữa
                val employeeInfo = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    ) {
                        append(data.employeeName)
                    }
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    ) {
                        append(" • Mã: ${data.employeeCode}")
                    }
                }

                Text(
                    text = employeeInfo,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Phòng ban & Giờ chấm công & Trạng thái đúng giờ/đi muộn
                val departmentInfo = buildAnnotatedString {
                    append("${data.department} • $actionText: ")
                    withStyle(
                        SpanStyle(
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(data.checkInTime)
                    }
                    append(" ")
                    withStyle(
                        SpanStyle(
                            color = if (data.attendanceStatus.contains("muộn", ignoreCase = true)) Color(0xFFEA580C) else primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("(${data.attendanceStatus})")
                    }
                }

                Text(
                    text = departmentInfo,
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Nút Tiếp tục bo tròn chuẩn Stitch
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .height(42.dp)
                ) {
                    Text(
                        text = "TIẾP TỤC",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dòng chữ đếm ngược tự động đóng sau 3s
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tự động đóng sau",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${remainingSeconds}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Thanh tiến trình Countdown Progress Bar dạng Pill nằm gọn bên trong Card
                val progress = (remainingSeconds / 3f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(lightBgColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Dialog thông báo Trùng lặp (Duplicate / Cooldown - Chuẩn Stitch Floating Modal)
 * - Màu vàng hổ phách (Amber) thân thiện, báo rõ mốc giờ đã chấm và số phút chờ
 */
@Composable
fun DuplicateStatusBanner(
    message: String,
    remainingSeconds: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amberPrimary = Color(0xFFD97706)
    val amberLight = Color(0xFFFEF3C7)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color.Black.copy(alpha = 0.22f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(28.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Gradient Amber -> White
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFEF3C7),
                                Color(0xFFFFFBEB),
                                Color.White
                            )
                        )
                    )
                    .padding(top = 22.dp, bottom = 8.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, amberPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Thông báo",
                        tint = amberPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Đã Ghi Nhận Chấm Công",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Không cần chấm lại liên tục",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = amberPrimary
                )
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = amberPrimary),
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .height(42.dp)
                ) {
                    Text(
                        text = "ĐÃ HIỂU",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tự động đóng sau",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${remainingSeconds}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = amberPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val progress = (remainingSeconds / 3f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(amberLight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(amberPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Dialog thông báo Thất bại (Floating Modal chuẩn Stitch Node 069c4a349ffa4ee19807ad28db18f844)
 * - Tự động đếm ngược 3s & Countdown Progress Bar bo góc gọn gàng không bị lòi viền
 */
@Composable
fun FailureStatusBanner(
    reason: String,
    remainingSeconds: Int,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color.Black.copy(alpha = 0.22f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFFFE4E6), RoundedCornerShape(28.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Gradient từ hồng nhạt (#FFF1F2) chuyển tiếp trắng
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF1F2),
                                Color(0xFFFFF5F5),
                                Color.White
                            )
                        )
                    )
                    .padding(top = 22.dp, bottom = 8.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Vòng tròn icon X đỏ viền 2.5dp
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, Color(0xFFF43F5E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Thất bại",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Chưa thể chấm công",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Không khớp thông tin nhận diện",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE11D48)
                )
            }

            // Body: Mô tả lý do và hàng nút hành động
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = reason.ifBlank { "Khuôn mặt chưa khớp hồ sơ TECNIX. Vui lòng căn chỉnh lại góc nhìn hoặc liên hệ quản lý." },
                    fontSize = 12.5.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Hàng nút: QUÉT LẠI (đỏ) & HỦY (xám)
                Row(
                    modifier = Modifier.fillMaxWidth(0.90f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(42.dp)
                    ) {
                        Text(
                            text = "QUÉT LẠI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF334155)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(0.8f)
                            .height(42.dp)
                    ) {
                        Text(
                            text = "Hủy",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dòng chữ đếm ngược tự động đóng sau 3s
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tự động đóng sau",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${remainingSeconds}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE11D48)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Thanh tiến trình Countdown Progress Bar dạng Pill nằm gọn bên trong Card không bị tràn mép
                val progress = (remainingSeconds / 3f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFFFE4E6))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFF43F5E))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
