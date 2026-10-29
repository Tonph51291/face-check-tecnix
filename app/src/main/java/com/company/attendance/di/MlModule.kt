package com.company.attendance.di

import android.content.Context
import com.company.attendance.domain.ml.FaceEmbedder

/**
 * Quản lý khởi tạo và chia sẻ thể hiện FaceEmbedder (TensorFlow Lite FaceNet 512D)
 * Hỗ trợ nạp trước (Pre-warm) từ SplashScreen để màn hình chính có sẵn mô hình trong RAM.
 */
object MlModule {

    @Volatile
    private var embedderInstance: FaceEmbedder? = null

    fun getFaceEmbedder(context: Context): FaceEmbedder {
        return embedderInstance ?: synchronized(this) {
            embedderInstance ?: FaceEmbedder(context.applicationContext).also {
                embedderInstance = it
            }
        }
    }

    /**
     * Nạp trước mô hình trong tiến trình nền
     */
    fun prewarm(context: Context) {
        getFaceEmbedder(context)
    }
}
