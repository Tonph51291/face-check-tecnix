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

    private val detector: FaceDetector = FaceDetection.getClient(options)
    private val isProcessing = java.util.concurrent.atomic.AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
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
                    onAnalysisResult(null)
                    return@addOnSuccessListener
                }

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
                onAnalysisResult(null)
            }
            .addOnCompleteListener {
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
