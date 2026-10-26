package com.company.attendance.presentation.common.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Component xử lý xin quyền Camera độc lập, tách rời logic khỏi Composable UI.
 */
@Composable
fun CameraPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onPermissionGranted() else onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            onPermissionGranted()
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
}

/**
 * Helper hook lấy trigger xin lại quyền camera (ví dụ bấm nút "Thử lại" / "Cấp quyền" trên Fallback UI)
 */
@Composable
fun rememberCameraPermissionLauncher(
    onResult: (Boolean) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }
    return { launcher.launch(Manifest.permission.CAMERA) }
}
