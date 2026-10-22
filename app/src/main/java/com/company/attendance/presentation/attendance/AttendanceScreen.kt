package com.company.attendance.presentation.attendance

import android.Manifest
import android.content.res.Configuration
import android.content.pm.PackageManager
import com.company.attendance.presentation.enrollment.EmployeeSearchScreen
import com.company.attendance.presentation.enrollment.EmployeeDetailScreen
import com.company.attendance.presentation.enrollment.FaceCaptureScreen
import com.company.attendance.presentation.enrollment.FaceReviewScreen
import com.company.attendance.domain.model.defaultFallbackEmployee
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.attendance.presentation.attendance.components.DigitalClockBar
import com.company.attendance.presentation.attendance.components.FaceGuideOverlay
import com.company.attendance.presentation.attendance.components.KioskHeader
import com.company.attendance.presentation.attendance.components.KioskSettingsBottomSheet
import com.company.attendance.presentation.common.components.CameraPreview
import com.company.attendance.presentation.common.components.FailureStatusBanner
import com.company.attendance.presentation.common.components.SuccessStatusBanner
import com.company.attendance.presentation.common.theme.ErrorRed
import com.company.attendance.presentation.common.theme.OnPrimary
import com.company.attendance.presentation.common.theme.OnSurfaceVariant
import com.company.attendance.presentation.common.theme.PrimaryBlue
import com.company.attendance.presentation.common.theme.Slate950
import com.company.attendance.presentation.common.theme.SurfaceCanvas

/**
 * Màn hình 1 – Chấm công chính (Attendance Kiosk All-in-One)
 * Tích hợp tự động phát hiện khuôn mặt thời gian thực (CameraX + Google ML Kit On-Device, Offline, 100% Miễn phí)
 */
@Composable
fun AttendanceScreen(
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launcher xin quyền camera lúc runtime
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onEvent(AttendanceUiEvent.OnCameraPermissionResult(isGranted))
    }

    LaunchedEffect(Unit) {
        val currentPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.onEvent(AttendanceUiEvent.OnCameraPermissionResult(currentPermission))
        if (!currentPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val activeEmployee = uiState.selectedEmployee ?: defaultFallbackEmployee

    when (uiState.currentScreen) {
        KioskScreen.ATTENDANCE -> {
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

                    // Khu vực Camera chính (Camera Viewfinder Box chuẩn Stitch)
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
                                    onRequestPermission = {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                )
                            }

                            uiState.status == RecognitionStatus.CAMERA_ERROR -> {
                                CameraErrorFallback(
                                    errorMessage = uiState.cameraErrorMessage ?: "Không thể kết nối đến camera.",
                                    onRetry = { viewModel.onEvent(AttendanceUiEvent.OnRetryCamera) }
                                )
                            }

                            else -> {
                                // CameraX Preview kèm phân tích frame bằng ML Kit tự động
                                CameraPreview(
                                    modifier = Modifier.fillMaxSize(),
                                    isFrontCamera = uiState.isFrontCamera,
                                    onFaceDetected = { faceData ->
                                        viewModel.onEvent(AttendanceUiEvent.OnFaceFrameAnalyzed(faceData))
                                    },
                                    onInitialized = {
                                        viewModel.onEvent(AttendanceUiEvent.OnCameraInitialized)
                                    },
                                    onError = { msg ->
                                        viewModel.onEvent(AttendanceUiEvent.OnCameraError(msg))
                                    }
                                )

                                // Lớp phủ Vignette Gradient công nghệ cao chuẩn Stitch
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.60f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                )

                                // Lớp phủ khung oval và trạng thái phát hiện khuôn mặt
                                FaceGuideOverlay(
                                    status = uiState.status,
                                    confidenceText = uiState.confidenceText,
                                    estimatedDistance = uiState.estimatedDistanceText,
                                    guidanceMessage = uiState.activeGuidance,
                                    stabilityProgress = uiState.stabilityProgress
                                )

                                // Dialog kết quả Thành công / Thất bại (Floating Modal chuẩn Stitch với nền mờ)
                                val isShowingResult = (uiState.status == RecognitionStatus.SUCCESS && uiState.resultData != null) ||
                                        uiState.status == RecognitionStatus.FAILURE

                                if (isShowingResult) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.status == RecognitionStatus.SUCCESS && uiState.resultData != null) {
                                            SuccessStatusBanner(
                                                data = uiState.resultData!!,
                                                remainingSeconds = uiState.autoDismissRemainingSeconds,
                                                onDismiss = {
                                                    viewModel.onEvent(AttendanceUiEvent.OnDismissResult)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth(0.92f)
                                                    .padding(horizontal = 16.dp)
                                            )
                                        } else if (uiState.status == RecognitionStatus.FAILURE) {
                                            FailureStatusBanner(
                                                reason = uiState.failureReason,
                                                remainingSeconds = uiState.autoDismissRemainingSeconds,
                                                onRetry = {
                                                    viewModel.onEvent(AttendanceUiEvent.OnScanAgain)
                                                },
                                                onDismiss = {
                                                    viewModel.onEvent(AttendanceUiEvent.OnDismissResult)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth(0.92f)
                                                    .padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isSettingsDialogVisible) {
                KioskSettingsBottomSheet(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onDismiss = {
                        viewModel.onEvent(AttendanceUiEvent.OnToggleSettingsDialog(false))
                    }
                )
            }
        }

        KioskScreen.EMPLOYEE_SEARCH -> {
            EmployeeSearchScreen(
                onNavigateBack = {
                    viewModel.onEvent(AttendanceUiEvent.OnNavigateToScreen(KioskScreen.ATTENDANCE))
                },
                onEmployeeSelected = { employeeId ->
                    viewModel.onEvent(AttendanceUiEvent.OnSelectEmployee(employeeId))
                }
            )
        }

        KioskScreen.EMPLOYEE_DETAIL -> {
            val employeeId = uiState.selectedEmployeeId ?: activeEmployee.code.ifBlank { activeEmployee.id }
            EmployeeDetailScreen(
                employeeId = employeeId,
                onNavigateBack = {
                    viewModel.onEvent(AttendanceUiEvent.OnNavigateToScreen(KioskScreen.EMPLOYEE_SEARCH))
                },
                onStartFaceCapture = {
                    viewModel.onEvent(AttendanceUiEvent.OnNavigateToScreen(KioskScreen.FACE_CAPTURE))
                }
            )
        }

        KioskScreen.FACE_CAPTURE -> {
            FaceCaptureScreen(
                employee = activeEmployee,
                onNavigateBack = {
                    viewModel.onEvent(AttendanceUiEvent.OnNavigateToScreen(KioskScreen.EMPLOYEE_DETAIL))
                },
                onCaptureFace = {
                    viewModel.onEvent(AttendanceUiEvent.OnCaptureFaceSample)
                }
            )
        }

        KioskScreen.FACE_REVIEW -> {
            FaceReviewScreen(
                employee = activeEmployee,
                onNavigateBack = {
                    viewModel.onEvent(AttendanceUiEvent.OnNavigateToScreen(KioskScreen.EMPLOYEE_DETAIL))
                },
                onRetakeSample = {
                    viewModel.onEvent(AttendanceUiEvent.OnNavigateToScreen(KioskScreen.FACE_CAPTURE))
                },
                onConfirmEnrollment = { empId ->
                    viewModel.onEvent(AttendanceUiEvent.OnConfirmEnrollment(empId))
                }
            )
        }
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
