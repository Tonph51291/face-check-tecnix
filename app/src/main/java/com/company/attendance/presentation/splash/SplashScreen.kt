package com.company.attendance.presentation.splash

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.di.MlModule
import com.example.facechecktecnix.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Màn hình Splash Kiosk đơn giản, nền trắng tinh tế và hiện đại.
 * Thực hiện nạp trước (Pre-warm) AI và Camera trong background trước khi chuyển vào màn chính.
 */
@Composable
fun SplashScreen(
    onInitializationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var progressTarget by remember { mutableFloatStateOf(0.1f) }
    var currentStepText by remember { mutableStateOf("Đang khởi tạo hệ thống...") }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "SplashProgress"
    )

    // Luồng khởi tạo ngầm các tác vụ
    LaunchedEffect(Unit) {
        delay(150)

        // 1. Nạp trước mô hình FaceNet TFLite 24MB vào RAM
        currentStepText = "Đang nạp mô hình nhận diện AI..."
        progressTarget = 0.40f
        withContext(Dispatchers.IO) {
            try {
                MlModule.prewarm(context)
            } catch (_: Exception) {}
        }
        delay(200)

        // 2. Khởi tạo sẵn CameraProvider
        currentStepText = "Khởi tạo cảm biến camera..."
        progressTarget = 0.75f
        withContext(Dispatchers.Main) {
            try {
                ProcessCameraProvider.getInstance(context)
            } catch (_: Exception) {}
        }
        delay(200)

        // 3. Hoàn tất sẵn sàng
        currentStepText = "Hệ thống đã sẵn sàng!"
        progressTarget = 1.0f
        delay(400)

        onInitializationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White) // Nền trắng tinh tế, sạch sẽ
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Khoảng đệm trên
            Spacer(modifier = Modifier.height(20.dp))

            // Khối trung tâm: Logo, Tên thương hiệu và Thanh tải
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Phượng hoàng TECNIX
                Image(
                    painter = painterResource(id = R.drawable.logo_tecnix),
                    contentDescription = "TECNIX Logo",
                    modifier = Modifier.size(150.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Tên thương hiệu TECNIX 2 màu chuẩn nhận diện (TEC Xanh Dương, NIX Cam)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEC",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0082C8), // Xanh dương
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "NIX",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF26522), // Cam
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Phụ đề
                Text(
                    text = "FaceCheck AI Kiosk",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2563EB),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(42.dp))

                // Thanh tiến trình loading mảnh, thanh lịch
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0284C7), // Xanh dương
                                        Color(0xFFEA580C)  // Cam đồng bộ logo
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trạng thái tải đơn giản
                Text(
                    text = currentStepText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }

            // Phần dưới: Thông tin phiên bản tinh gọn
            Text(
                text = "Phiên bản 2.4.0 • TECNIX Biometrics",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
        }
    }
}
