package com.company.attendance.presentation.attendance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.attendance.AttendanceUiEvent
import com.company.attendance.presentation.attendance.AttendanceUiState
import com.company.attendance.presentation.common.theme.ErrorRed
import com.company.attendance.presentation.common.theme.OnError
import com.company.attendance.presentation.common.theme.OnPrimary
import com.company.attendance.presentation.common.theme.OnSurfaceText
import com.company.attendance.presentation.common.theme.OnSurfaceVariant
import com.company.attendance.presentation.common.theme.OnTertiary
import com.company.attendance.presentation.common.theme.PrimaryBlue
import com.company.attendance.presentation.common.theme.SuccessGreen
import com.company.attendance.presentation.common.theme.SurfaceContainer
import com.company.attendance.presentation.common.theme.SurfaceContainerLow
import com.company.attendance.presentation.common.theme.SurfaceContainerLowest

/**
 * Modal BottomSheet Cài đặt Kiosk, Thông tin Mô hình Offline & Kiểm thử
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KioskSettingsBottomSheet(
    uiState: AttendanceUiState,
    onEvent: (AttendanceUiEvent) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToEmployeeList: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = OnPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Cài đặt Kiosk TECNIX",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceText
                        )
                        Text(
                            text = "Quản trị Camera & Thông số AI On-Device",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Thẻ trạng thái Model AI Offline & Chi phí 0đ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE8F5EE))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Mô hình On-Device: Google ML Kit Face Detection",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF005230)
                        )
                        Text(
                            text = "• Chi phí: 100% Miễn phí (Apache 2.0 License)\n• Tải đóng gói sẵn (Bundled APK), chạy Offline không cần Internet\n• Phân tích frame CameraX tự động, không gửi ảnh lên Cloud",
                            fontSize = 11.sp,
                            color = Color(0xFF003820),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Box Danh sách nhân viên (Chuyển sang Màn hình 3: Tìm kiếm nhân viên chuẩn Stitch)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEFF6FF))
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(16.dp))
                    .clickable {
                        onNavigateToEmployeeList?.invoke() ?: onEvent(AttendanceUiEvent.OnToggleEmployeeSearch(true))
                        onDismiss()
                    }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Danh sách nhân viên",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(PrimaryBlue.copy(alpha = 0.12f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Màn hình 3",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                            Text(
                                text = "Tra cứu, đối soát hồ sơ & đăng ký mẫu khuôn mặt mới",
                                fontSize = 11.5.sp,
                                color = Color(0xFF2563EB),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Mở",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Điều khiển chuyển đổi Camera Trước / Sau
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .padding(14.dp)
            ) {
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Camera đang sử dụng",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceText
                            )
                            Text(
                                text = if (uiState.isFrontCamera) "Camera trước (Mặc định)" else "Camera sau",
                                fontSize = 12.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { onEvent(AttendanceUiEvent.OnSwitchCamera) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(
                            text = if (uiState.isFrontCamera) "Dùng Cam sau" else "Dùng Cam trước",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mô phỏng kiểm thử UI độc lập
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Mô phỏng kiểm thử giao diện (Testing Sandbox)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Kích hoạt nhanh trạng thái hiển thị UI:",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onEvent(AttendanceUiEvent.OnSimulateSuccess)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Thành công", fontSize = 11.sp, color = OnTertiary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onEvent(AttendanceUiEvent.OnSimulateFailure)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Thất bại", fontSize = 11.sp, color = OnError, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onEvent(AttendanceUiEvent.OnSimulateIdle)
                                onDismiss()
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Quét lại", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cảnh báo minh bạch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFEF7EC))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFB7791F),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Lưu ý bảo mật: Đã kích hoạt bộ lọc căn chỉnh và kiểm tra tính sống cơ bản (mắt, góc đầu). Mô hình nhận diện danh tính (trích xuất vector đặc trưng) và chống giả mạo chuyên sâu cần nạp model weights vào assets/models trước khi vào vận hành thực tế.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B4500),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
