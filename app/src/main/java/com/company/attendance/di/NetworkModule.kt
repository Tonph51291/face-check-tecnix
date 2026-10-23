package com.company.attendance.di

import com.company.attendance.data.remote.api.AttendanceApi
import com.company.attendance.data.remote.api.EmployeeApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cấu hình chung cho toàn bộ kết nối API (Network Config) của hệ thống Kiosk FaceCheckTecnix.
 * Kết nối trực tiếp tới Backend FaceCheck AI Service chạy local.
 */
object NetworkModule {

    /**
     * Địa chỉ Backend FaceCheck AI Service:
     * - Khi chạy trên Android Emulator: dùng "http://10.0.2.2:8000/" (10.0.2.2 trỏ về localhost máy tính host)
     * - Khi chạy trên Tablet/Điện thoại thật cùng mạng Wi-Fi: thay bằng IP LAN máy tính, ví dụ: "http://192.168.1.100:8000/"
     */
    var BASE_URL: String = "http://10.0.2.2:8000/"

    /**
     * API Key xác thực Kiosk (khớp với mục auth.api_keys trong config.yaml của backend)
     */
    const val API_KEY: String = "dev-kiosk-key-change-me"

    // 1. Interceptor gắn Header Authorization Bearer và Content-Type vào mọi request
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Authorization", "Bearer $API_KEY")
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
