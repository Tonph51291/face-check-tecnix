package com.company.attendance.presentation.common.components

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.company.attendance.data.ml.CameraFaceAnalyzer
import android.graphics.Bitmap
import com.company.attendance.domain.model.FaceDetectionData

/**
 * CameraPreview hiển thị video trực tiếp từ CameraX và phân tích khuôn mặt thời gian thực qua ImageAnalysis
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false,
    onFaceDetected: (FaceDetectionData?) -> Unit = {},
    onCaptureReady: ((() -> Bitmap?) -> Unit)? = null,
    onInitialized: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var currentAnalyzer: CameraFaceAnalyzer? = remember { null }
    val backgroundExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    DisposableEffect(isFrontCamera, lifecycleOwner) {
        onDispose {
            try {
                currentAnalyzer?.close()
                backgroundExecutor.shutdown()
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (_: Exception) {
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }

                onCaptureReady?.invoke {
                    previewView.bitmap
                }

                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    try {

                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()

                        val primarySelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        val fallbackSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }

                        val selectorToUse = if (cameraProvider.hasCamera(primarySelector)) {
                            primarySelector
                        } else if (cameraProvider.hasCamera(fallbackSelector)) {
                            fallbackSelector
                        } else {
                            onError("Không tìm thấy thiết bị camera khả dụng trên hệ thống.")
                            return@addListener
                        }

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val analyzer = CameraFaceAnalyzer { faceData ->
                            onFaceDetected(faceData)
                        }
                        currentAnalyzer = analyzer

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()
                            .also {
                                it.setAnalyzer(backgroundExecutor, analyzer)
                            }

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selectorToUse,
                            preview,
                            imageAnalysis
                        )
                        onInitialized()
                    } catch (e: Exception) {
                        onError("Lỗi khởi tạo camera: ${e.localizedMessage ?: "Không xác định"}")
                    }
                }, executor)

                previewView
            }
        )
    }
}
