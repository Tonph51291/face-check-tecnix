import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Đọc cấu hình từ file .env ở thư mục gốc của project
val envFile = rootProject.file(".env")
val envProperties = Properties().apply {
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}

val kioskBaseUrl = envProperties.getProperty("KIOSK_BASE_URL", "http://10.0.2.2:8080/")
val kioskDeviceApiKey = envProperties.getProperty("KIOSK_DEVICE_API_KEY", "a7f8c92b4e6d10f3a5b8c9d2e1f4a7b0c3d6e9f2a5b8c1d4e7f0a3b6c9d2e5f8")
val kioskDeviceSerial = envProperties.getProperty("KIOSK_DEVICE_SERIAL", "KIOSK-01")

android {
    namespace = "com.example.facechecktecnix"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.facechecktecnix"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject biến từ .env vào BuildConfig của Android
        buildConfigField("String", "KIOSK_BASE_URL", "\"$kioskBaseUrl\"")
        buildConfigField("String", "KIOSK_DEVICE_API_KEY", "\"$kioskDeviceApiKey\"")
        buildConfigField("String", "KIOSK_DEVICE_SERIAL", "\"$kioskDeviceSerial\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Face Detection (Bundled On-Device, Offline, 100% Free Apache 2.0)
    implementation(libs.mlkit.face.detection)

    // Networking (Retrofit, OkHttp, Gson)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}