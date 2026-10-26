package com.example.facechecktecnix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.company.attendance.presentation.common.theme.AttendanceKioskTheme
import com.company.attendance.presentation.navigation.KioskApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendanceKioskTheme {
                KioskApp()
            }
        }
    }
}