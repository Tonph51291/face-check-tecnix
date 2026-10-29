package com.company.attendance.core.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [DEBUG_CAPTURE_TEST]
 * Helper phục vụ kiểm tra và đánh giá độ nét ảnh chụp khuôn mặt:
 * - In đầy đủ thông số kỹ thuật (Kích thước width x height, dung lượng RAM, dung lượng file).
 * - Lưu file ảnh .jpg vào bộ nhớ máy để người dùng có thể mở xem trực tiếp độ nét.
 * - In lệnh adb pull để kéo ảnh về máy tính kiểm tra.
 */
object DebugCaptureHelper {

    private const val TAG = "FACE_CAPTURE_DEBUG"

    // Lưu lại bitmap chụp gần nhất trong RAM để hiển thị preview trên màn hình test
    var latestCapturedBitmap: Bitmap? = null
        private set

    fun saveAndLogCapturedBitmap(context: Context, bitmap: Bitmap): File? {
        latestCapturedBitmap = bitmap

        val width = bitmap.width
        val height = bitmap.height
        val ramKb = bitmap.byteCount / 1024

        Log.i(TAG, "==========================================================================")
        Log.i(TAG, "📸 [FACE_CAPTURE] BẮT ĐẦU CHỤP KHUNG HÌNH TỪ CAMERA")
        Log.i(TAG, "📐 Độ phân giải ảnh chụp thực tế: ${width} x ${height} px")
        Log.i(TAG, "💾 Dung lượng bộ nhớ RAM: $ramKb KB")

        return try {
            // Lưu vào thư mục Pictures của ứng dụng (người dùng mở được bằng Device File Explorer hoặc ứng dụng Tệp)
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            if (!picturesDir.exists()) {
                picturesDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
            val debugFile = File(picturesDir, "test_capture_latest.jpg")
            val historyFile = File(picturesDir, "test_capture_$timestamp.jpg")

            // Ghi file với chất lượng JPEG cao (95%) để giữ nguyên độ nét gốc
            FileOutputStream(debugFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                fos.flush()
            }

            // Ghi một bản lưu vết lịch sử
            FileOutputStream(historyFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                fos.flush()
            }

            val fileKb = debugFile.length() / 1024

            Log.i(TAG, "✅ [FACE_CAPTURE] ĐÃ LƯU FILE ẢNH TEST THÀNH CÔNG:")
            Log.i(TAG, "📁 File ảnh mới nhất: ${debugFile.absolutePath}")
            Log.i(TAG, "📁 File ảnh lịch sử:  ${historyFile.absolutePath}")
            Log.i(TAG, "📦 Dung lượng file JPEG (chất lượng 95%): $fileKb KB")
            Log.i(TAG, "💻 LỆNH KÉO ẢNH VỀ MÁY TÍNH XEM ĐỘ NÉT:")
            Log.i(TAG, "   adb pull \"${debugFile.absolutePath}\" .")
            Log.i(TAG, "==========================================================================")

            debugFile
        } catch (e: Exception) {
            Log.e(TAG, "❌ [FACE_CAPTURE] Lỗi khi lưu file ảnh: ${e.message}", e)
            null
        }
    }
}
