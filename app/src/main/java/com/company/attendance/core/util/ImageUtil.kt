package com.company.attendance.core.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Tiện ích xử lý dữ liệu hình ảnh (chuyển đổi định dạng Base64, nén JPEG, căn chỉnh kích thước).
 */
object ImageUtil {

    /**
     * Chuyển đổi Bitmap sang chuỗi Base64 chuẩn JPEG.
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Giảm kích thước ảnh nếu vượt quá maxDimension để tối ưu băng thông mạng và tốc độ AI.
     */
    fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int = 1080): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = max(width, height)
        if (maxSide <= maxDimension) {
            return bitmap
        }

        val scale = maxDimension.toFloat() / maxSide
        val matrix = Matrix().apply {
            postScale(scale, scale)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }
}

/**
 * Extension function tiện dụng cho Bitmap
 */
fun Bitmap.toBase64Jpeg(quality: Int = 85, maxDimension: Int = 1080): String {
    val scaledBitmap = ImageUtil.downscaleIfNeeded(this, maxDimension)
    return ImageUtil.bitmapToBase64(scaledBitmap, quality)
}
