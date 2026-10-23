package com.company.attendance.presentation.enrollment

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.components.BiometricCapturedViewport
import com.company.attendance.presentation.enrollment.components.EnrollmentSyncModal
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.QualityMetricBox
import kotlinx.coroutines.delay

/**
 * Màn hình Xem lại & Đánh giá mẫu sinh trắc học chuẩn Stitch (Review Sample):
 * - Bố cục chuẩn xác theo bản thiết kế Stitch (Screen ID: 19524645cfee45ee953da0fa6723910d).
 * - Lưới 3 thẻ chỉ số chất lượng: Độ sắc nét (98%), Ánh sáng (320 Lux), Góc nhìn (0° Trực diện).
 * - Thẻ hình ảnh mẫu đã chụp (FacialPreviewCard) với hiệu ứng kính mờ (Glassmorphism), thông tin nhân sự và icon sinh trắc học.
 * - Hộp cảnh báo thông báo trạng thái sẵn sàng lưu trữ.
 * - Thanh hành động chân trang: Nút "Hoàn tất đăng ký" nổi bật, 2 nút phụ "Chụp lại mẫu" và "Hủy bỏ", cùng Home indicator.
 */
@Composable
fun FaceReviewScreen(
    employee: Employee,
    onNavigateBack: () -> Unit,
    onRetakeSample: () -> Unit,
    onConfirmEnrollment: (String) -> Unit,
    modifier: Modifier = Modifier,
    capturedBitmap: Bitmap? = null
) {
    var isSaving by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFFF8FAFC) // slate-50
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP APP BAR
            EnrollmentTopBar(
                title = "TECNIX KIOSK",
                subtitle = "Kiểm tra mẫu sinh trắc học",
                onNavigateBack = onNavigateBack
            )

            // 2. NỘI DUNG CUỘN CHÍNH
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // LƯỚI 3 THẺ CHỈ SỐ CHẤT LƯỢNG (Quality Metrics 3-Grid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QualityMetricBox(
                            label = "Độ sắc nét",
                            value = "98%",
                            status = "Tối ưu",
                            icon = Icons.Default.CenterFocusStrong,
                            iconColor = Color(0xFF059669), // emerald-600
                            modifier = Modifier.weight(1f)
                        )
                        QualityMetricBox(
                            label = "Ánh sáng",
                            value = "Đạt",
                            status = "320 Lux",
                            icon = Icons.Default.WbSunny,
                            iconColor = Color(0xFFF59E0B), // amber-500
                            modifier = Modifier.weight(1f)
                        )
                        QualityMetricBox(
                            label = "Góc nhìn",
                            value = "0°",
                            status = "Trực diện",
                            icon = Icons.Default.Straighten,
                            iconColor = Color(0xFF6366F1), // indigo-500
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // THẺ XEM TRƯỚC HÌNH ẢNH MẪU ĐÃ CHỤP (FacialPreviewCard)
                    BiometricCapturedViewport(
                        employee = employee,
                        capturedBitmap = capturedBitmap
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // HỘP THÔNG BÁO TRẠNG THÁI (StatusAlert)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCCEFF6FF)) // blue-50/80
                            .border(width = 1.dp, color = Color(0xB3BFDBFE), shape = RoundedCornerShape(16.dp)) // blue-200/70
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2563EB), // brand-600
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 1.dp)
                            )
                            Column {
                                Text(
                                    text = "Ảnh mẫu đã sẵn sàng lưu trữ",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B) // slate-800
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Ảnh chụp đã vượt qua bài kiểm tra liveness và chất lượng. Dữ liệu chưa lưu vào máy chủ. Vui lòng nhấn \"Hoàn tất đăng ký\" để lưu trữ mẫu mã hóa vào hệ thống Kiosk.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF475569), // slate-600
                                    lineHeight = 16.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // 3. THANH HÀNH ĐỘNG CHÂN TRANG (ActionButtons)
            ReviewBottomActionsToolbar(
                onConfirm = { isSaving = true },
                onRetake = onRetakeSample,
                onCancel = onNavigateBack
            )
        }

        // Modal phản hồi khi bấm "Hoàn tất đăng ký"
        if (isSaving) {
            LaunchedEffect(Unit) {
                delay(1200)
                isSaving = false
                onConfirmEnrollment(employee.id)
            }

            EnrollmentSyncModal()
        }
    }
}

/**
 * Thanh nút bấm hành động chân trang chuẩn Stitch:
 * - Nút Hoàn tất đăng ký (Primary Solid Brand Blue với icon tick bo tròn)
 * - 2 Nút phụ bên dưới: Chụp lại mẫu & Hủy bỏ
 * - Home indicator bar
 */
@Composable
private fun ReviewBottomActionsToolbar(
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .shadow(elevation = 4.dp, spotColor = Color(0x0F000000))
            .border(width = 1.dp, color = Color(0xFFE2E8F0))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nút Hoàn tất đăng ký chính (Primary brand-600)
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x332563EB)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB), // brand-600
                    contentColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hoàn tất đăng ký",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2 Nút phụ: Chụp lại mẫu & Hủy bỏ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Chụp lại mẫu
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(14.dp))
                        .clickable { onRetake() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = Color(0xFF64748B), // slate-500
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chụp lại mẫu",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155) // slate-700
                        )
                    }
                }

                // Hủy bỏ
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(14.dp))
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFE11D48), // rose-600
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hủy bỏ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE11D48) // rose-600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Home Indicator Bar
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    }
}
