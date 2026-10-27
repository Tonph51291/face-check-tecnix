package com.company.attendance.di

import com.company.attendance.data.remote.api.AttendanceApi
import com.company.attendance.data.remote.api.EmployeeApi
import com.example.facechecktecnix.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cấu hình chung cho toàn bộ kết nối API (Network Config) của hệ thống Kiosk FaceCheckTecnix.
 * Tự động đọc cấu hình động từ file .env qua BuildConfig.
 */
object NetworkModule {

    /**
     * Địa chỉ Backend Golang tecnix-office (nạp tự động từ .env qua BuildConfig):
     * - Khi chạy trên Android Emulator: http://10.0.2.2:8080/
     * - Khi chạy trên Tablet thật: http://<IP_LAN>:8080/
     */
    var BASE_URL: String = BuildConfig.KIOSK_BASE_URL

    /**
     * API Key xác thực Kiosk (nạp tự động từ .env qua BuildConfig)
     */
    val DEVICE_API_KEY: String = BuildConfig.KIOSK_DEVICE_API_KEY

    /**
     * Mã serial máy Kiosk
     */
    val DEVICE_SERIAL: String = BuildConfig.KIOSK_DEVICE_SERIAL

    // 1. Interceptor gắn Header X-Device-Key và Accept JSON theo đúng đặc tả Dual-Auth của backend
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("X-Device-Key", DEVICE_API_KEY)
            .header("Accept", "application/json")
            .method(original.method, original.body)
        chain.proceed(requestBuilder.build())
    }

    // 2. Logging Interceptor giúp kiểm tra Request/Response ngay trong Logcat Android Studio
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 3. OkHttpClient dùng chung với timeout 10 giây (theo đúng spec dự án)
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // 4. Retrofit Instance dùng chung
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 5. Cung cấp EmployeeApi Service
    val employeeApi: EmployeeApi by lazy {
        retrofit.create(EmployeeApi::class.java)
    }

    // 6. Cung cấp AttendanceApi Service
    val attendanceApi: AttendanceApi by lazy {
        retrofit.create(AttendanceApi::class.java)
    }
}
