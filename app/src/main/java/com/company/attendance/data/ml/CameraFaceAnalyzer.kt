package com.company.attendance.data.ml

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.company.attendance.domain.ml.FaceQualityChecker
import com.company.attendance.domain.model.FaceAlignmentStatus
import com.company.attendance.domain.model.FaceDetectionData
import com.company.attendance.domain.model.FaceTrackItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * CameraFaceAnalyzer: Phân tích từng frame CameraX bằng Google ML Kit Face Detection.
 * Hỗ trợ nhận diện nhiều khuôn mặt (Multi-Face Tracking), tự động chọn người ưu tiên.
 */
class CameraFaceAnalyzer(
    private val qualityChecker: FaceQualityChecker = FaceQualityChecker(),
    private val onAnalysisResult: (FaceDetectionData?) -> Unit
) : ImageAnalysis.Analyzer {

    // Cấu hình ML Kit Face Detector:
    // - Bật CLASSIFICATION_MODE_ALL để kiểm tra đặc trưng mắt (loại bỏ nhiễu tường/vật thể vô tri, chống chớp mắt)
    // - minFaceSize = 0.08f (8%) mở rộng khoảng cách bắt mặt linh hoạt từ 0.5m đến 2.0m
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.08f)
        .enableTracking()
        .build()

    companion object {
        // Dynamic FPS Throttle:
        // - Khi phòng trống (chưa có người): giới hạn ~5 FPS (mỗi 200ms) để giảm 80-85% tải CPU
        // - Khi đang phát hiện & bám bắt mặt: tăng tốc lên ~25 FPS (mỗi 40ms) để bắt mặt siêu tốc tức thì
        private const val IDLE_FRAME_INTERVAL_MS = 200L
        private const val ACTIVE_FRAME_INTERVAL_MS = 40L
    }

    private val detector: FaceDetector = FaceDetection.getClient(options)
    private val isProcessing = java.util.concurrent.atomic.AtomicBoolean(false)
    private var lastAnalyzedTimestampMs = 0L
    @Volatile
    private var hadFacesLastFrame = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = android.os.SystemClock.elapsedRealtime()
        val minInterval = if (hadFacesLastFrame) ACTIVE_FRAME_INTERVAL_MS else IDLE_FRAME_INTERVAL_MS
        if (now - lastAnalyzedTimestampMs < minInterval) {
            imageProxy.close()
            return
        }

        // Chỉ xử lý 1 frame tại 1 thời điểm, tự động bỏ qua frame cũ (zero latency)
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Tính kích thước ảnh sau khi đã xoay
        val isRotated = (rotationDegrees == 90 || rotationDegrees == 270)
        val rotatedWidth = if (isRotated) imageProxy.height else imageProxy.width
        val rotatedHeight = if (isRotated) imageProxy.width else imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    hadFacesLastFrame = false
                    onAnalysisResult(null)
                    return@addOnSuccessListener
                }

                // Chuyển đổi và lọc bỏ các khuôn mặt giả lập (Ghost/Phantom Face) do nhiễu môi trường:
                // 1. Kích thước tối thiểu: Chiều rộng và chiều cao >= 60px (hỗ trợ bắt xa tới 2m)
                // 2. Tỉ lệ khung hình (Aspect Ratio = w / h): Khuôn mặt người thật luôn nằm trong khoảng 0.60 .. 1.45
                val trackedFaces = faces.mapNotNull { face ->
                    val box = face.boundingBox
                    val w = box.width()
                    val h = box.height()
                    if (w < 60 || h < 60) return@mapNotNull null
                    val aspectRatio = w.toFloat() / h.toFloat()
                    if (aspectRatio !in 0.60f..1.45f) return@mapNotNull null

                    FaceTrackItem(
                        boundingBox = box,
                        trackingId = face.trackingId,
                        headEulerAngleX = face.headEulerAngleX,
                        headEulerAngleY = face.headEulerAngleY,
                        headEulerAngleZ = face.headEulerAngleZ,
                        leftEyeOpenProbability = face.leftEyeOpenProbability,
                        rightEyeOpenProbability = face.rightEyeOpenProbability,
                        smilingProbability = face.smilingProbability
                    )
                }

                if (trackedFaces.isEmpty()) {
                    hadFacesLastFrame = false
                    onAnalysisResult(null)
                    return@addOnSuccessListener
                }

                hadFacesLastFrame = true

                val allBoundingBoxes = trackedFaces.map { it.boundingBox }

                // Chọn người ưu tiên (Primary Face):
                // 1. Ưu tiên khuôn mặt có đặc trưng hợp lệ (không phải đồ vật)
                // 2. Ưu tiên người đang nhìn thẳng (isLookingStraight)
                // 3. Ưu tiên người đang mở mắt (areEyesOpen - chống chụp lúc nhắm mắt)
                // 4. Ưu tiên người có diện tích lớn nhất (đứng gần camera nhất)
                val primaryFace = trackedFaces.filter { it.hasValidFaceFeatures }.maxWithOrNull(
                    compareBy<FaceTrackItem> { it.isLookingStraight }
                        .thenBy { it.areEyesOpen }
                        .thenBy { it.area }
                ) ?: trackedFaces.first()

                val evaluation = qualityChecker.evaluate(
                    boundingBox = primaryFace.boundingBox,
                    imageWidth = rotatedWidth,
                    imageHeight = rotatedHeight,
                    angleX = primaryFace.headEulerAngleX,
                    angleY = primaryFace.headEulerAngleY,
                    angleZ = primaryFace.headEulerAngleZ
                )

                onAnalysisResult(
                    FaceDetectionData(
                        boundingBox = primaryFace.boundingBox,
                        allFaces = allBoundingBoxes,
                        trackedFaces = trackedFaces,
                        primaryFace = primaryFace,
                        headEulerAngleX = primaryFace.headEulerAngleX,
                        headEulerAngleY = primaryFace.headEulerAngleY,
                        headEulerAngleZ = primaryFace.headEulerAngleZ,
                        leftEyeOpenProbability = primaryFace.leftEyeOpenProbability,
                        rightEyeOpenProbability = primaryFace.rightEyeOpenProbability,
                        smilingProbability = primaryFace.smilingProbability,
                        trackingId = primaryFace.trackingId,
                        imageWidth = rotatedWidth,
                        imageHeight = rotatedHeight,
                        rotationDegrees = rotationDegrees,
                        alignmentStatus = if (trackedFaces.size > 1) FaceAlignmentStatus.ALIGNED else evaluation.status,
                        guidanceMessage = evaluation.message
                    )
                )
            }
            .addOnFailureListener {
                hadFacesLastFrame = false
                onAnalysisResult(null)
            }
            .addOnCompleteListener {
                lastAnalyzedTimestampMs = android.os.SystemClock.elapsedRealtime()
                // Tuyệt đối luôn đóng imageProxy và giải phóng cờ để frame tiếp theo được nhận ngay lập tức
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    fun close() {
        try {
            detector.close()
        } catch (_: Exception) {
        }
    }
}
