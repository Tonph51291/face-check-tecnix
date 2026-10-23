package com.company.attendance.presentation.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors
import kotlinx.coroutines.delay

import android.graphics.Bitmap

/**
 * Màn hình 4B: Xem lại mẫu khuôn mặt (Biometric Sample Review & Commit - MVVM Refactored)
 * Node ID: 2d2430e1e6184d31b0b85aefdb0140fc
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
        color = EnrollmentColors.Surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP APP BAR
            EnrollmentTopBar(
                title = "TECNIX KIOSK",
                subtitle = "Kiểm tra mẫu sinh trắc học",
                badgeText = "Review Sample",
                onNavigateBack = onNavigateBack
            )

            // 2. BODY CONTENT
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                    ) {
                        // Tiêu đề & ID đăng ký
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Đánh giá mẫu sinh trắc",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EnrollmentColors.OnSurface
                                )
                                Text(
                                    text = "Bước 2/2: Đánh giá chất lượng sinh trắc học",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EnrollmentColors.Primary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(EnrollmentColors.SurfaceContainerHigh)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "ID: #REG-8092",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EnrollmentColors.OnSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // THẺ ĐÁNH GIÁ CHẤT LƯỢNG MẪU (Quality Assessment Card)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFEDF0FF), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(EnrollmentColors.TertiaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Đã chụp mẫu khuôn mặt thành công",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EnrollmentColors.OnSurface
                                            )
                                            Text(
                                                text = "Phù hợp tiêu chuẩn ISO/IEC 19794-5",
                                                fontSize = 11.5.sp,
                                                color = EnrollmentColors.OnSurfaceVariant
                                            )
                                        }
                                    }

                                    // Điểm số
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFFE8F8F0))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = EnrollmentColors.Tertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "98/100 Điểm",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EnrollmentColors.Tertiary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 3 Ô thông số đo đạc chất lượng
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QualityMetricBox(
                                        label = "Độ sắc nét",
                                        value = "98%",
                                        status = "Tối ưu",
                                        icon = Icons.Default.CenterFocusStrong,
                                        modifier = Modifier.weight(1f)
                                    )
                                    QualityMetricBox(
                                        label = "Ánh sáng",
                                        value = "Đạt",
                                        status = "320 Lux",
                                        icon = Icons.Default.WbSunny,
                                        modifier = Modifier.weight(1f)
                                    )
                                    QualityMetricBox(
                                        label = "Góc nhìn",
                                        value = "0°",
                                        status = "Trực diện",
                                        icon = Icons.Default.Straighten,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // KHUNG HÌNH MẪU ĐÃ CHỤP VỚI OVERLAY HUD SINH TRẮC HỌC
                        BiometricCapturedViewport(
                            employee = employee,
                            capturedBitmap = capturedBitmap
                        )


                        Spacer(modifier = Modifier.height(14.dp))

                        // THÔNG BÁO TRẠNG THÁI MẪU SẴN SÀNG LƯU
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(EnrollmentColors.SurfaceContainerHigh.copy(alpha = 0.6f))
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = EnrollmentColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Ảnh mẫu đã sẵn sàng lưu trữ",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EnrollmentColors.OnSurface
                                    )
                                    Text(
                                        text = "Ảnh chụp đã vượt qua bài kiểm tra liveness và chất lượng. Dữ liệu chưa lưu vào máy chủ. Vui lòng nhấn \"Hoàn tất đăng ký\" để lưu trữ mẫu mã hóa vào hệ thống Kiosk.",
                                        fontSize = 12.sp,
                                        color = EnrollmentColors.OnSurfaceVariant,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // CỤM NÚT HÀNH ĐỘNG CHÍNH
                        Button(
                            onClick = { isSaving = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EnrollmentColors.Primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hoàn tất đăng ký",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2 Nút phụ: Chụp lại mẫu & Hủy bỏ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRetakeSample,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = EnrollmentColors.OnSurface
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Chụp lại mẫu", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = EnrollmentColors.SurfaceContainerLow,
                                    contentColor = Color(0xFFBA1A1A)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Hủy bỏ", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
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
