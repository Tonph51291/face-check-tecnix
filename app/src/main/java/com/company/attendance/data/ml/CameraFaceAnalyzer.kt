package com.company.attendance.data.ml

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.company.attendance.domain.ml.FaceQualityChecker
import com.company.attendance.domain.model.FaceAlignmentStatus
import com.company.attendance.domain.model.FaceDetectionData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * CameraFaceAnalyzer: Phân tích từng frame CameraX bằng Google ML Kit Face Detection (On-Device, Offline, Apache 2.0).
 * Quản lý vòng đời frame, tự động điều tiết tốc độ (throttling) để bảo vệ CPU/pin của thiết bị Kiosk.
 */
class CameraFaceAnalyzer(
    private val qualityChecker: FaceQualityChecker = FaceQualityChecker(),
    private val onAnalysisResult: (FaceDetectionData?) -> Unit
) : ImageAnalysis.Analyzer {

    // Cấu hình ML Kit Face Detector tối ưu cho Kiosk realtime video
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.12f)
        .enableTracking()
        .build()

    private val detector: FaceDetector = FaceDetection.getClient(options)

    // Throttling: giới hạn tối đa 7-8 frame/giây (~130ms/frame) để tránh quá tải nhiệt
    private var lastAnalyzedTimestamp = 0L
    private val throttleIntervalMs = 130L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < throttleIntervalMs) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
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
                lastAnalyzedTimestamp = currentTimestamp

                if (faces.isEmpty()) {
                    onAnalysisResult(null)
                    return@addOnSuccessListener
                }

                if (faces.size > 1) {
                    val primaryFace = faces[0]
                    onAnalysisResult(
                        FaceDetectionData(
                            boundingBox = primaryFace.boundingBox,
                            headEulerAngleX = primaryFace.headEulerAngleX,
                            headEulerAngleY = primaryFace.headEulerAngleY,
                            headEulerAngleZ = primaryFace.headEulerAngleZ,
                            trackingId = primaryFace.trackingId,
                            imageWidth = rotatedWidth,
                            imageHeight = rotatedHeight,
                            rotationDegrees = rotationDegrees,
                            alignmentStatus = FaceAlignmentStatus.MULTIPLE_FACES,
                            guidanceMessage = "Phát hiện nhiều người. Vui lòng chỉ một người đứng trước camera."
                        )
                    )
                    return@addOnSuccessListener
                }

                val face = faces[0]
                val evaluation = qualityChecker.evaluate(
                    boundingBox = face.boundingBox,
                    imageWidth = rotatedWidth,
                    imageHeight = rotatedHeight,
                    angleX = face.headEulerAngleX,
                    angleY = face.headEulerAngleY,
                    angleZ = face.headEulerAngleZ
                )

                onAnalysisResult(
                    FaceDetectionData(
                        boundingBox = face.boundingBox,
                        headEulerAngleX = face.headEulerAngleX,
                        headEulerAngleY = face.headEulerAngleY,
                        headEulerAngleZ = face.headEulerAngleZ,
                        leftEyeOpenProbability = face.leftEyeOpenProbability,
                        rightEyeOpenProbability = face.rightEyeOpenProbability,
                        smilingProbability = face.smilingProbability,
                        trackingId = face.trackingId,
                        imageWidth = rotatedWidth,
                        imageHeight = rotatedHeight,
                        rotationDegrees = rotationDegrees,
                        alignmentStatus = evaluation.status,
                        guidanceMessage = evaluation.message
                    )
                )
            }
            .addOnFailureListener {
                onAnalysisResult(null)
            }
            .addOnCompleteListener {
                // Tuyệt đối luôn đóng imageProxy trong mọi tình huống để không nghẽn CameraX pipeline
                imageProxy.close()
            }
    }

    fun close() {
        try {
            detector.close()
        } catch (_: Exception) {
        }
    }
}
