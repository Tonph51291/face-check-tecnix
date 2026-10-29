package com.company.attendance.presentation.attendance

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.attendance.presentation.attendance.components.AttendanceResultDock
import com.company.attendance.presentation.attendance.components.DigitalClockBar
import com.company.attendance.presentation.attendance.components.KioskHeader
import com.company.attendance.presentation.attendance.components.KioskSettingsBottomSheet
import com.company.attendance.presentation.common.components.CameraPreview
import com.company.attendance.presentation.common.components.FaceDetectionOverlay
import com.company.attendance.presentation.common.permission.CameraPermissionHandler
import com.company.attendance.presentation.common.permission.rememberCameraPermissionLauncher
import com.company.attendance.presentation.common.theme.ErrorRed
import com.company.attendance.presentation.common.theme.OnPrimary
import com.company.attendance.presentation.common.theme.PrimaryBlue
import com.company.attendance.presentation.common.theme.SurfaceCanvas

/**
 * Màn hình 1 – Chấm công chính (Attendance Kiosk All-in-One)
 * Tích hợp tự động phát hiện khuôn mặt thời gian thực (CameraX + Google ML Kit On-Device, Offline, 100% Miễn phí).
 * Phân tách rõ ràng trách nhiệm: Chỉ phụ trách UI và tương tác chấm công, không kiêm nhiệm Router điều hướng.
 */
@Composable
fun AttendanceScreen(
    onNavigateToEmployeeSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 1. Tách logic xin quyền Camera độc lập qua Permission Handler
    CameraPermissionHandler(
        onPermissionGranted = {
            viewModel.onEvent(AttendanceUiEvent.OnCameraPermissionResult(true))
        },
        onPermissionDenied = {
            viewModel.onEvent(AttendanceUiEvent.OnCameraPermissionResult(false))
        }
    )

    val requestCameraPermission = rememberCameraPermissionLauncher { isGranted ->
        viewModel.onEvent(AttendanceUiEvent.OnCameraPermissionResult(isGranted))
    }

    // Lắng nghe sự kiện nếu có yêu cầu mở màn hình tìm kiếm từ nội bộ ViewModel
    LaunchedEffect(uiState.isEmployeeSearchVisible) {
        if (uiState.isEmployeeSearchVisible) {
            onNavigateToEmployeeSearch()
            viewModel.onEvent(AttendanceUiEvent.OnToggleEmployeeSearch(false))
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = if (isLandscape) 1200.dp else 780.dp)
                .padding(horizontal = if (isLandscape) 20.dp else 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thanh tiêu đề Kiosk gọn gàng (Logo TECNIX, Trạng thái AI, Nút cài đặt)
            KioskHeader(
                onOpenSettings = {
                    viewModel.onEvent(AttendanceUiEvent.OnToggleSettingsDialog(true))
                },
                resolutionStatusText = if (uiState.hasCameraPermission && uiState.isCameraInitialized) {
                    if (uiState.isRealMode) "AI Online" else "Demo Mode"
                } else if (!uiState.hasCameraPermission) {
                    "Chờ cấp quyền"
                } else {
                    "Đang kết nối"
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Thẻ thời gian và thông tin chấm công (bố cục 2 hàng rõ ràng, thời gian nổi bật, không vỡ chữ)
            DigitalClockBar(
                timeString = uiState.currentTimeString,
                dateString = uiState.currentDateString,
                shiftName = uiState.shiftName,
                statusText = "Chấm công tự động",
                isOnline = uiState.hasCameraPermission && uiState.isCameraInitialized
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Khu vực Camera chính (Camera Viewfinder Box)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0F1D))
                    .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                when {
                    !uiState.hasCameraPermission -> {
                        CameraPermissionFallback(
                            onRequestPermission = requestCameraPermission
                        )
                    }

                    uiState.status == RecognitionStatus.CAMERA_ERROR -> {
                        CameraErrorFallback(
                            errorMessage = uiState.cameraErrorMessage ?: "Không thể kết nối đến camera.",
                            onRetry = { viewModel.onEvent(AttendanceUiEvent.OnRetryCamera) }
                        )
                    }

                    else -> {
                        var fastDetectedFaces by remember { mutableStateOf<List<Rect>>(emptyList()) }
                        var fastFrameWidth by remember { mutableIntStateOf(0) }
                        var fastFrameHeight by remember { mutableIntStateOf(0) }
                        var fastPrimaryBox by remember { mutableStateOf<Rect?>(null) }

                        // CameraX Preview kèm phân tích frame bằng ML Kit tự động (High-speed 60 FPS)
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            isFrontCamera = uiState.isFrontCamera,
                            onFaceDetected = { faceData ->
                                // Cập nhật ngay lập tức lên UI local state để khung ngắm bám sát 100% thời gian thực không độ trễ
                                fastDetectedFaces = faceData?.allFaces ?: emptyList()
                                fastFrameWidth = faceData?.imageWidth ?: 0
                                fastFrameHeight = faceData?.imageHeight ?: 0
                                fastPrimaryBox = faceData?.boundingBox
                                viewModel.onEvent(AttendanceUiEvent.OnFaceFrameAnalyzed(faceData))
                            },
                            onCaptureReady = { provider ->
                                viewModel.setBitmapProvider(provider)
                            },
                            onInitialized = {
                                viewModel.onEvent(AttendanceUiEvent.OnCameraInitialized)
                            },
                            onError = { msg ->
                                viewModel.onEvent(AttendanceUiEvent.OnCameraError(msg))
                            }
                        )

                        // Lớp phủ vẽ góc ngắm khuôn mặt thời gian thực (bám sát khuôn mặt không độ trễ)
                        FaceDetectionOverlay(
                            faces = fastDetectedFaces,
                            imageWidth = fastFrameWidth,
                            imageHeight = fastFrameHeight,
                            primaryFaceBox = fastPrimaryBox,
                            isFrontCamera = uiState.isFrontCamera,
                            isMultipleFaces = fastDetectedFaces.size > 1
                        )

                        // Thẻ thông báo kết quả & trạng thái gắn đáy không che camera (Non-blocking Bottom Dock)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            AttendanceResultDock(
                                status = uiState.status,
                                resultData = uiState.resultData,
                                duplicateMessage = uiState.duplicateMessage,
                                failureReason = uiState.failureReason,
                                capturedBitmap = uiState.capturedBitmap
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isSettingsDialogVisible) {
        KioskSettingsBottomSheet(
            onEvent = viewModel::onEvent,
            onDismiss = {
                viewModel.onEvent(AttendanceUiEvent.OnToggleSettingsDialog(false))
            },
            onNavigateToEmployeeList = {
                viewModel.onEvent(AttendanceUiEvent.OnToggleSettingsDialog(false))
                onNavigateToEmployeeSearch()
            }
        )
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Yêu cầu quyền truy cập Camera",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ứng dụng chấm công Kiosk cần quyền truy cập camera để tự động phát hiện khuôn mặt nhân viên.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text(
                text = "Cấp quyền Camera",
                fontWeight = FontWeight.SemiBold,
                color = OnPrimary
            )
        }
    }
}

@Composable
private fun CameraErrorFallback(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ErrorRed.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Không thể khởi động Camera",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thử lại",
                fontWeight = FontWeight.SemiBold,
                color = OnPrimary
            )
        }
    }
}
