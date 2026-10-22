package com.company.attendance.core.logging

import android.util.Log

/**
 * Tiện ích Logging dùng chung cho toàn bộ dự án Kiosk FaceCheckTecnix (AppLogger).
 *
 * Tính năng nổi bật:
 * 1. Tự động nhận diện Tên File / Tên Class / Số dòng gọi log (không cần truyền Tag thủ công).
 * 2. Hỗ trợ biểu tượng cảm xúc trực quan trong Logcat: 🔍 Debug, ℹ️ Info, ⚠️ Warning, ❌ Error, 🌐 API.
 * 3. Hỗ trợ in đẹp JSON, Exception Throwable.
 * 4. Cung cấp cả hàm gọi qua Object `AppLogger.d(...)` lẫn các hàm tiện ích toàn cục `logD(...)`, `logE(...)`.
 */
object AppLogger {

    private const val DEFAULT_TAG = "FaceCheck"
    private const val MAX_LOG_LENGTH = 4000

    /**
     * Cờ bật/tắt log. Có thể cấu hình tắt khi build Release.
     */
    var isEnabled: Boolean = true

    /**
     * Log Debug - Dùng cho thông tin debug khi phát triển (🔍)
     */
    fun d(message: String, tag: String? = null, throwable: Throwable? = null) {
        if (!isEnabled) return
        val finalTag = tag ?: createAutoTag()
        val formattedMsg = "🔍 $message"
        if (throwable != null) {
            Log.d(finalTag, formattedMsg, throwable)
        } else {
            logSplit(Log.DEBUG, finalTag, formattedMsg)
        }
    }

    /**
     * Log Info - Dùng cho thông tin tiến trình nghiệp vụ bình thường (ℹ️)
     */
    fun i(message: String, tag: String? = null, throwable: Throwable? = null) {
        if (!isEnabled) return
        val finalTag = tag ?: createAutoTag()
        val formattedMsg = "ℹ️ $message"
        if (throwable != null) {
            Log.i(finalTag, formattedMsg, throwable)
        } else {
            logSplit(Log.INFO, finalTag, formattedMsg)
        }
    }

    /**
     * Log Warning - Dùng cho cảnh báo rủi ro nhưng chưa gây crash (⚠️)
     */
    fun w(message: String, tag: String? = null, throwable: Throwable? = null) {
        if (!isEnabled) return
        val finalTag = tag ?: createAutoTag()
        val formattedMsg = "⚠️ $message"
        if (throwable != null) {
            Log.w(finalTag, formattedMsg, throwable)
        } else {
            logSplit(Log.WARN, finalTag, formattedMsg)
        }
    }

    /**
     * Log Error - Dùng cho ngoại lệ và sự cố nghiêm trọng (❌)
     */
    fun e(message: String, throwable: Throwable? = null, tag: String? = null) {
        if (!isEnabled) return
        val finalTag = tag ?: createAutoTag()
        val formattedMsg = "❌ $message"
        if (throwable != null) {
            Log.e(finalTag, formattedMsg, throwable)
        } else {
            logSplit(Log.ERROR, finalTag, formattedMsg)
        }
    }

    /**
     * Log API - Dành riêng để theo dõi Request & Response mạng (🌐)
     */
    fun api(endpoint: String, status: String, details: String = "", tag: String? = null) {
        if (!isEnabled) return
        val finalTag = tag ?: "FaceCheck-API"
        val message = "🌐 [$status] $endpoint ${if (details.isNotEmpty()) "-> $details" else ""}"
        Log.i(finalTag, message)
    }

    /**
     * Tự động lấy Tên Class và Số Dòng nơi gọi hàm log từ StackTrace
     * Ví dụ: "EmployeeRepositoryImpl:35"
     */
    private fun createAutoTag(): String {
        val stackTrace = Throwable().stackTrace
        if (stackTrace.size < 3) return DEFAULT_TAG

        // Tìm frame đầu tiên bên ngoài AppLogger
        val callerFrame = stackTrace.firstOrNull { frame ->
            !frame.className.contains(AppLogger::class.java.simpleName) &&
            !frame.className.contains("AppLoggerKt") &&
            !frame.className.startsWith("android.") &&
            !frame.className.startsWith("java.")
        } ?: stackTrace[2]

        val simpleClassName = callerFrame.className.substringAfterLast('.')
        return "$DEFAULT_TAG-$simpleClassName:${callerFrame.lineNumber}"
    }

    /**
     * Xử lý cắt chuỗi nếu nội dung log vượt quá giới hạn 4000 ký tự của Logcat
     */
    private fun logSplit(priority: Int, tag: String, message: String) {
        if (message.length <= MAX_LOG_LENGTH) {
            Log.println(priority, tag, message)
            return
        }

        var i = 0
        while (i < message.length) {
            val end = (i + MAX_LOG_LENGTH).coerceAtMost(message.length)
            Log.println(priority, tag, message.substring(i, end))
            i = end
        }
    }
}

// =========================================================================
// Các hàm tiện ích mở rộng toàn cục (Global Functions) để gõ ngắn gọn nhất
// =========================================================================

/** Gọi nhanh: logD("Dữ liệu là: $data") */
fun logD(message: String, tag: String? = null, throwable: Throwable? = null) {
    AppLogger.d(message, tag, throwable)
}

/** Gọi nhanh: logI("Đã tải xong nhân viên") */
fun logI(message: String, tag: String? = null, throwable: Throwable? = null) {
    AppLogger.i(message, tag, throwable)
}

/** Gọi nhanh: logW("Mạng chậm bất thường") */
fun logW(message: String, tag: String? = null, throwable: Throwable? = null) {
    AppLogger.w(message, tag, throwable)
}

/** Gọi nhanh: logE("Lỗi kết nối", exception) */
fun logE(message: String, throwable: Throwable? = null, tag: String? = null) {
    AppLogger.e(message, throwable, tag)
}

/** Gọi nhanh: logApi("/api/v1/employees", "200 OK", "8 nhân viên") */
fun logApi(endpoint: String, status: String, details: String = "", tag: String? = null) {
    AppLogger.api(endpoint, status, details, tag)
}
