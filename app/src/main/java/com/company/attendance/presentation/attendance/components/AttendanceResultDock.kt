package com.company.attendance.presentation.attendance.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.attendance.AttendanceResultData
import com.company.attendance.presentation.attendance.RecognitionStatus

/**
 * Bottom Result Dock (Thẻ thông báo kết quả & trạng thái gắn đáy Kiosk):
 * - Thay thế hoàn toàn Dialog modal che màn hình cũ.
 * - Camera chạy liên tục 100% không bị ngắt quãng hay phủ đen.
 * - Hiển thị tên nhân viên, mã nhân viên, phòng ban và thời gian chấm công nổi bật.
 * - Hỗ trợ liên tục: Người tiếp theo bước vào có thể quét ngay lập tức.
 */
@Composable
fun AttendanceResultDock(
    status: RecognitionStatus,
    resultData: AttendanceResultData?,
    duplicateMessage: String?,
    failureReason: String?,
    capturedBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = status,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(260)) { it / 2 })
                .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(200)) { it / 2 })
        },
        label = "attendance_dock_state",
        modifier = modifier
    ) { currentStatus ->
        when {
            // 1. TRƯỜNG HỢP CHẤM CÔNG THÀNH CÔNG: Thẻ xanh sang trọng hiện thông tin nhân viên
            currentStatus == RecognitionStatus.SUCCESS && resultData != null -> {
                SuccessResultCard(data = resultData, capturedBitmap = capturedBitmap)
            }

            // 2. TRƯỜNG HỢP TRÙNG LẶP (ĐÃ CHẤM GẦN ĐÂY): Thẻ cảnh báo màu vàng hổ phách
            currentStatus == RecognitionStatus.DUPLICATE -> {
                DuplicateResultCard(message = duplicateMessage)
            }

            // 3. TRƯỜNG HỢP THẤT BẠI: Thẻ thông báo đỏ dịu không làm gián đoạn camera
            currentStatus == RecognitionStatus.FAILURE -> {
                FailureResultCard(reason = failureReason)
            }

            // 4. TRƯỜNG HỢP ĐANG GỬI DỮ LIỆU XÁC THỰC
            currentStatus == RecognitionStatus.VERIFYING -> {
                VerifyingCard()
            }

            // 5. TRẠNG THÁI CHỜ / QUÉT TỰ DO BÌNH THƯỜNG
            else -> {
                IdleStatusPill(status = currentStatus)
            }
        }
    }
}

/**
 * Thẻ thông báo thành công gắn đáy: Tên nhân viên to rõ, phòng ban, giờ chấm công
 */
@Composable
private fun SuccessResultCard(
    data: AttendanceResultData,
    capturedBitmap: Bitmap? = null
) {
    val isCheckOut = data.attendanceType.equals("CHECK_OUT", ignoreCase = true)
    val accentColor = if (isCheckOut) Color(0xFF0284C7) else Color(0xFF10B981)
    val actionLabel = if (isCheckOut) "TAN CA" else "VÀO CA"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = accentColor.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF20F172A)) // Nền kính tối công nghệ cao
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(accentColor, accentColor.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // [DEBUG_CAPTURE_TEST] Hiển thị trực tiếp ảnh vừa chụp để kiểm tra độ nét, nếu không có thì hiện icon tích
            if (capturedBitmap != null) {
                Image(
                    bitmap = capturedBitmap.asImageBitmap(),
                    contentDescription = "Ảnh chụp test",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .shadow(8.dp, CircleShape, spotColor = accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Thành công",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Khối thông tin nhân viên
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CHẤM CÔNG THÀNH CÔNG",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "• ${data.checkInTime}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Tên nhân viên to, in đậm, cực kỳ dễ đọc từ xa
                Text(
                    text = data.employeeName.uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Mã nhân viên và phòng ban
                Text(
                    text = "${data.employeeCode}  •  ${data.department}",
                    fontSize = 12.5.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Chip phân loại Vào ca / Tan ca
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = data.attendanceStatus,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * Thẻ thông báo trùng lặp (vừa chấm xong)
 */
@Composable
private fun DuplicateResultCard(message: String?) {
    val amberColor = Color(0xFFF59E0B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF20F172A))
            .border(1.5.dp, amberColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(amberColor.copy(alpha = 0.2f))
                    .border(1.5.dp, amberColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Đã chấm",
                    tint = amberColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BẠN VỪA HOÀN TẤT CHẤM CÔNG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = amberColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message ?: "Vui lòng nhường vị trí cho người tiếp theo.",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Thẻ thông báo không nhận diện được
 */
@Composable
private fun FailureResultCard(reason: String?) {
    val redColor = Color(0xFFEF4444)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF20F172A))
            .border(1.5.dp, redColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(redColor.copy(alpha = 0.2f))
                    .border(1.5.dp, redColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Thất bại",
                    tint = redColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CHƯA NHẬN DIỆN ĐƯỢC DANH TÍNH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = redColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reason ?: "Khuôn mặt chưa được đăng ký trong hệ thống nhân sự.",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Thẻ hiển thị khi đang đối chiếu khuôn mặt qua AI Server
 */
@Composable
private fun VerifyingCard() {
    val cyanColor = Color(0xFF38BDF8)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF20F172A))
            .border(1.dp, cyanColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = cyanColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ĐANG ĐỐI CHIẾU DỮ LIỆU AI...",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = cyanColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Vui lòng giữ yên một tích tắc",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Thanh trạng thái ngầm tối giản khi Kiosk đang chờ người tiếp theo
 */
@Composable
private fun IdleStatusPill(status: RecognitionStatus) {
    val isFaceDetected = status == RecognitionStatus.FACE_DETECTED || status == RecognitionStatus.FACE_STABLE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0F172A))
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (isFaceDetected) Color(0xFF38BDF8) else Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isFaceDetected) "Đã nhận diện khuôn mặt • Đang quét..." else "Hệ thống AI sẵn sàng • Nhìn vào camera",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            Text(
                text = "Tự động nhận diện",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
