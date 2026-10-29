package com.example.facechecktecnix

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.company.attendance.presentation.common.theme.AttendanceKioskTheme
import com.company.attendance.presentation.navigation.KioskApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Giữ màn hình luôn sáng liên tục cho thiết bị Kiosk 24/7
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            AttendanceKioskTheme {
                KioskApp()
            }
        }
    }
}