package com.company.attendance.domain.ml

import android.graphics.Bitmap
import com.company.attendance.domain.model.FaceDetectionData
import com.company.attendance.domain.model.FaceQualityMetrics
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Bộ phân tích và đo đạc chất lượng hình ảnh khuôn mặt thực tế (On-Device Computer Vision):
 * - Đo độ sắc nét thông qua toán tử phương sai biến thiên Laplace (Laplacian Variance).
 * - Đo độ sáng trung bình quang học (Luminance BT.601) và ước lượng Lux môi trường.
 * - Đo độ lệch góc mặt thực tế (Pitch & Yaw Euler Angles) từ ML Kit.
 */
object BitmapQualityAnalyzer {

    fun analyze(bitmap: Bitmap?, faceData: FaceDetectionData?): FaceQualityMetrics {
        if (bitmap == null) {
            return FaceQualityMetrics()
        }

        // 1. Phân tích Góc nhìn từ dữ liệu ML Kit thực tế
        val yaw = abs(faceData?.headEulerAngleY ?: 0f)
        val pitch = abs(faceData?.headEulerAngleX ?: 0f)
        val angleDegree = sqrt(yaw * yaw + pitch * pitch).roundToInt().coerceIn(0, 45)

        val angleStatus = when {
            angleDegree <= 6 -> "Trực diện"
            angleDegree <= 15 -> "Nghiêng nhẹ"
            else -> "Nghiêng nhiều"
        }

        // 2. Chuẩn bị mẫu ảnh để tính toán sắc nét & ánh sáng
        // Lấy vùng trọng tâm khuôn mặt để phân tích, tránh ảnh hưởng bởi hậu cảnh
        val sampleSize = 160
        val sampleBitmap = extractFaceRegion(bitmap, faceData, sampleSize)

        val width = sampleBitmap.width
        val height = sampleBitmap.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        sampleBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val grayPixels = IntArray(totalPixels)
        var totalLuminance = 0L

        // Chuyển sang Grayscale và tính tổng độ sáng
        for (i in 0 until totalPixels) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            grayPixels[i] = lum
            totalLuminance += lum
        }

        // 3. Phân tích Ánh sáng & Ước lượng Lux
        val avgLuminance = if (totalPixels > 0) (totalLuminance / totalPixels).toFloat() else 128f
        val lightLux = ((avgLuminance / 255.0f) * 550f).roundToInt().coerceIn(60, 850)

        val lightStatus = when {
            avgLuminance < 65f -> "Thiếu sáng"
            avgLuminance in 90f..170f -> "Tối ưu"
            avgLuminance in 65f..200f -> "Đạt"
            else -> "Chói sáng"
        }

        // 4. Phân tích Độ sắc nét (Laplacian Variance)
        // Áp dụng kernel Laplace 4-hướng: [0, 1, 0; 1, -4, 1; 0, 1, 0]
        var laplacianSum = 0.0
        var laplacianSqSum = 0.0
        var edgePixelsCount = 0

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val center = grayPixels[rowOffset + x]
                val up = grayPixels[rowOffset - width + x]
                val down = grayPixels[rowOffset + width + x]
                val left = grayPixels[rowOffset + x - 1]
                val right = grayPixels[rowOffset + x + 1]

                val laplacian = (up + down + left + right - 4 * center).toDouble()
                laplacianSum += laplacian
                laplacianSqSum += laplacian * laplacian
                edgePixelsCount++
            }
        }

        val laplacianVariance = if (edgePixelsCount > 0) {
            val mean = laplacianSum / edgePixelsCount
            (laplacianSqSum / edgePixelsCount) - (mean * mean)
        } else {
            300.0
        }

        // Quy đổi Variance sang thang điểm % sắc nét thực tế
        val sharpnessPercent = when {
            laplacianVariance >= 500.0 -> (96 + min(3, ((laplacianVariance - 500) / 250).toInt())).coerceIn(96, 99)
            laplacianVariance >= 250.0 -> (88 + ((laplacianVariance - 250.0) / 250.0 * 8).toInt()).coerceIn(88, 95)
            laplacianVariance >= 120.0 -> (76 + ((laplacianVariance - 120.0) / 130.0 * 11).toInt()).coerceIn(76, 87)
            laplacianVariance >= 50.0 -> (60 + ((laplacianVariance - 50.0) / 70.0 * 15).toInt()).coerceIn(60, 75)
            else -> (45 + (laplacianVariance / 50.0 * 14).toInt()).coerceIn(40, 59)
        }

        val sharpnessStatus = when {
            sharpnessPercent >= 88 -> "Tối ưu"
            sharpnessPercent >= 75 -> "Đạt"
            else -> "Bị mờ"
        }

        // 5. Đánh giá tổng thể & Tạo câu khuyến nghị chính xác cho người dùng
        val isOverallPass = sharpnessPercent >= 70 && lightStatus != "Thiếu sáng" && lightStatus != "Chói sáng" && angleDegree <= 20

        val (recTitle, recMessage) = when {
            sharpnessStatus == "Bị mờ" -> Pair(
                "Ảnh mẫu hơi mờ",
                "Độ sắc nét chưa tối ưu (${sharpnessPercent}%). Bạn có thể bấm \"Chụp lại mẫu\" để camera lấy nét rõ hơn hoặc tiếp tục nếu muốn."
            )
            lightStatus == "Thiếu sáng" -> Pair(
                "Ánh sáng hơi yếu",
                "Môi trường chụp hơi tối (~${lightLux} Lux). Nên hướng thêm ánh sáng vào mặt để nhận diện tốt nhất, hoặc bấm \"Hoàn tất đăng ký\" nếu đã hài lòng."
            )
            lightStatus == "Chói sáng" -> Pair(
                "Ánh sáng chói lóa",
                "Có nguồn sáng mạnh chiếu trực diện vào camera. Bạn nên đổi góc đứng một chút để ảnh không bị lóa."
            )
            angleStatus == "Nghiêng nhiều" -> Pair(
                "Góc nghiêng khá lớn (${angleDegree}°)",
                "Khuôn mặt đang lệch góc ${angleDegree}°. Mẫu trực diện sẽ giúp thuật toán chấm công đạt độ chính xác cao nhất."
            )
            else -> Pair(
                "Ảnh mẫu đã sẵn sàng lưu trữ",
                "Ảnh chụp đã vượt qua bài kiểm tra chất lượng thực tế: Độ sắc nét $sharpnessPercent%, ánh sáng $lightLux Lux và góc nhìn $angleDegree° đều đạt chuẩn sinh trắc học tối ưu."
            )
        }

        return FaceQualityMetrics(
            sharpnessPercent = sharpnessPercent,
            sharpnessStatus = sharpnessStatus,
            lightLux = lightLux,
            lightStatus = lightStatus,
            angleDegree = angleDegree,
            angleStatus = angleStatus,
            isOverallPass = isOverallPass,
            recommendationTitle = recTitle,
            recommendationMessage = recMessage
        )
    }

    private fun extractFaceRegion(bitmap: Bitmap, faceData: FaceDetectionData?, targetSize: Int): Bitmap {
        return try {
            val bmpWidth = bitmap.width
            val bmpHeight = bitmap.height

            val (cropX, cropY, cropW, cropH) = if (faceData != null && faceData.boundingBox.width() > 50 && faceData.boundingBox.height() > 50) {
                val box = faceData.boundingBox
                val x = box.left.coerceIn(0, bmpWidth - 1)
                val y = box.top.coerceIn(0, bmpHeight - 1)
                val w = box.width().coerceAtMost(bmpWidth - x)
                val h = box.height().coerceAtMost(bmpHeight - y)
                listOf(x, y, w, h)
            } else {
                // Nếu chưa có box chuẩn, cắt 50% vùng trung tâm ảnh
                val w = (bmpWidth * 0.5f).toInt()
                val h = (bmpHeight * 0.5f).toInt()
                val x = (bmpWidth - w) / 2
                val y = (bmpHeight - h) / 2
                listOf(x, y, w, h)
            }

            val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
            Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        } catch (_: Exception) {
            Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        }
    }
}
