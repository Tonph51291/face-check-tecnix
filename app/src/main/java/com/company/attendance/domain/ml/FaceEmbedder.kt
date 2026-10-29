package com.company.attendance.domain.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Base64
import com.company.attendance.core.logging.logE
import com.company.attendance.core.logging.logI
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Trích xuất vector đặc trưng khuôn mặt 512 chiều (FaceNet Inception-ResNet-v1) qua TensorFlow Lite.
 * - Input: Khuôn mặt crop từ Bitmap camera, scale về 160x160 RGB float32 [-1, 1].
 * - Output: Vector 512 chiều đã chuẩn hóa L2 norm (||v|| = 1.0).
 */
class FaceEmbedder(private val context: Context) {

    private var interpreter: Interpreter? = null

    companion object {
        const val MODEL_FILE = "face_recongize.tflite"
        const val INPUT_SIZE = 160
        const val EMBEDDING_DIM = 512
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 128.0f
    }

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            logI("Đã nạp thành công mô hình FaceNet TFLite (512D) từ assets")
        } catch (e: Exception) {
            logE("Không thể nạp mô hình $MODEL_FILE từ assets", e)
        }
    }

    /**
     * Dò tìm trực tiếp vị trí khuôn mặt trên chính Bitmap chụp được (độ chính xác 100%, không bị lệch tỷ lệ View hay camera stream)
     */
    suspend fun detectFaceBoundingBox(bitmap: Bitmap): Rect? = suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
            val detector = FaceDetection.getClient(options)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    val primary = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    if (continuation.isActive) {
                        continuation.resume(primary?.boundingBox)
                    }
                    detector.close()
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                    detector.close()
                }
        } catch (_: Exception) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    /**
     * Chuyển đổi tọa độ khuôn mặt từ luồng camera sensor sang tọa độ View hiển thị thực tế (xử lý FILL_CENTER & lật gương camera trước)
     */
    fun mapBoundingBoxToBitmap(
        sensorBox: Rect,
        sensorWidth: Int,
        sensorHeight: Int,
        bitmapWidth: Int,
        bitmapHeight: Int,
        isFrontCamera: Boolean = false
    ): Rect {
        if (sensorWidth <= 0 || sensorHeight <= 0 || bitmapWidth <= 0 || bitmapHeight <= 0) {
            return sensorBox
        }

        val scale = maxOf(bitmapWidth.toFloat() / sensorWidth, bitmapHeight.toFloat() / sensorHeight)
        val scaledW = sensorWidth * scale
        val scaledH = sensorHeight * scale
        val offsetX = (bitmapWidth - scaledW) / 2f
        val offsetY = (bitmapHeight - scaledH) / 2f

        val left: Float
        val right: Float
        if (isFrontCamera) {
            left = bitmapWidth - (sensorBox.right * scale + offsetX)
            right = bitmapWidth - (sensorBox.left * scale + offsetX)
        } else {
            left = sensorBox.left * scale + offsetX
            right = sensorBox.right * scale + offsetX
        }
        val top = sensorBox.top * scale + offsetY
        val bottom = sensorBox.bottom * scale + offsetY

        val finalLeft = maxOf(0, minOf(left, right).toInt())
        val finalTop = maxOf(0, minOf(top, bottom).toInt())
        val finalRight = minOf(bitmapWidth, maxOf(left, right).toInt())
        val finalBottom = minOf(bitmapHeight, maxOf(top, bottom).toInt())

        return Rect(finalLeft, finalTop, finalRight, finalBottom)
    }

    /**
     * Cắt ảnh khuôn mặt từ Bitmap với lề biên an toàn cân đối (mặc định 20% mỗi bên)
     */
    fun cropFace(bitmap: Bitmap, box: Rect, marginRatio: Float = 0.20f): Bitmap? {
        val marginX = (box.width() * marginRatio).toInt()
        val marginY = (box.height() * marginRatio).toInt()

        val cropLeft = maxOf(0, box.left - marginX)
        val cropTop = maxOf(0, box.top - marginY)
        val cropWidth = minOf(bitmap.width - cropLeft, box.width() + 2 * marginX)
        val cropHeight = minOf(bitmap.height - cropTop, box.height() + 2 * marginY)

        if (cropWidth <= 10 || cropHeight <= 10) return null
        return Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
    }

    /**
     * Trích xuất vector 512 chiều từ ảnh mặt đã cắt chuẩn xác
     */
    fun extractEmbeddingFromCroppedFace(croppedFace: Bitmap): FloatArray? {
        val interp = interpreter ?: run {
            logE("FaceEmbedder: Model chưa được khởi tạo")
            return null
        }

        try {
            val resized = Bitmap.createScaledBitmap(croppedFace, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
                rewind()
            }

            val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
            resized.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
            if (resized != croppedFace) {
                resized.recycle()
            }

            for (pixel in intValues) {
                val r = (pixel shr 16 and 0xFF)
                val g = (pixel shr 8 and 0xFF)
                val b = (pixel and 0xFF)
                inputBuffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat((b - IMAGE_MEAN) / IMAGE_STD)
            }

            val outputArray = Array(1) { FloatArray(EMBEDDING_DIM) }
            interp.run(inputBuffer, outputArray)
            return l2Normalize(outputArray[0])
        } catch (e: Exception) {
            logE("FaceEmbedder: Lỗi trích xuất vector khuôn mặt", e)
            return null
        }
    }

    /**
     * Trích xuất vector 512D từ vùng khuôn mặt (tương thích ngược)
     */
    fun extractEmbedding(bitmap: Bitmap, boundingBox: Rect): FloatArray? {
        val cropped = cropFace(bitmap, boundingBox, marginRatio = 0.10f) ?: return null
        val vector = extractEmbeddingFromCroppedFace(cropped)
        cropped.recycle()
        return vector
    }

    /**
     * Tạo ảnh thumbnail Avatar Base64 từ ảnh mặt đã crop (kèm prefix data:image/jpeg;base64,)
     */
    fun createThumbnailBase64FromCroppedFace(croppedFace: Bitmap, targetSize: Int = 300): String? {
        try {
            val resized = Bitmap.createScaledBitmap(croppedFace, targetSize, targetSize, true)
            val stream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            if (resized != croppedFace) {
                resized.recycle()
            }

            val rawBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            return "data:image/jpeg;base64,$rawBase64"
        } catch (e: Exception) {
            logE("FaceEmbedder: Lỗi tạo thumbnail avatar", e)
            return null
        }
    }

    /**
     * Tạo ảnh thumbnail crop nhỏ khuôn mặt (tương thích ngược)
     */
    fun createThumbnailBase64(bitmap: Bitmap, boundingBox: Rect, targetSize: Int = 300): String? {
        val cropped = cropFace(bitmap, boundingBox, marginRatio = 0.18f) ?: return null
        val base64 = createThumbnailBase64FromCroppedFace(cropped, targetSize)
        cropped.recycle()
        return base64
    }

    private fun l2Normalize(v: FloatArray): FloatArray {
        var sumSquares = 0.0
        for (x in v) {
            sumSquares += (x * x).toDouble()
        }
        val norm = sqrt(sumSquares).toFloat()
        if (norm <= 1e-6f) return v
        for (i in v.indices) {
            v[i] /= norm
        }
        return v
    }

    /**
     * Tính toán độ tương đồng Cosine Similarity giữa 2 vector [-1.0 .. 1.0]
     */
    fun calculateCosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 0f
        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }
        val denom = (sqrt(norm1.toDouble()) * sqrt(norm2.toDouble())).toFloat()
        return if (denom > 1e-6f) dot / denom else 0f
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
        } catch (_: Exception) {}
    }
}
