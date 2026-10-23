package com.company.attendance.presentation.enrollment.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Bảng màu Design System chuẩn Stitch cho màn hình Quản lý nhân sự Kiosk.
 * Khớp 100% với mã màu Tailwind từ Stitch Design (Tailwind slate & brand colors).
 */
object EnrollmentColors {
    // Nền & Chữ cơ bản
    val Surface = Color(0xFFF8FAFC)          // slate-50
    val CardBg = Color(0xFFFFFFFF)           // white
    val OnSurface = Color(0xFF0F172A)        // slate-900
    val OnSurfaceSecondary = Color(0xFF334155)// slate-700
    val OnSurfaceVariant = Color(0xFF64748B) // slate-500
    val Outline = Color(0xFF94A3B8)          // slate-400
    val OutlineVariant = Color(0xFFE2E8F0)   // slate-200
    val BorderLight = Color(0xFFF1F5F9)      // slate-100

    // Thương hiệu TECNIX / Brand Blue
    val Primary = Color(0xFF2563EB)          // brand-600
    val PrimaryDark = Color(0xFF1D4ED8)      // brand-700
    val PrimaryLight = Color(0xFFEEF4FF)     // brand-50
    val PrimaryBorder = Color(0xFFDBEAFE)    // blue-100
    val PrimaryContainer = Color(0xFF2457C5)
    val PrimaryFixed = Color(0xFFDBE1FF)
    val OnPrimaryFixed = Color(0xFF001849)

    // Bộ màu trạng thái Đã có mẫu mặt (Emerald)
    val SuccessTagBg = Color(0xFFECFDF5)     // emerald-50
    val SuccessTagBorder = Color(0xFFA7F3D0) // emerald-200
    val SuccessTagText = Color(0xFF047857)   // emerald-700
    val SuccessIcon = Color(0xFF059669)      // emerald-600

    // Bộ màu trạng thái Chưa có mẫu mặt (Rose)
    val WarningTagBg = Color(0xFFFFF1F2)     // rose-50
    val WarningTagBorder = Color(0xFFFECDD3) // rose-200
    val WarningTagText = Color(0xFFBE123C)   // rose-700
    val WarningIcon = Color(0xFFE11D48)      // rose-600

    // Lỗi hệ thống
    val Error = Color(0xFFBE123C)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFF1F2)
    val OnErrorContainer = Color(0xFFBE123C)

    // Nhóm màu Tertiary (sinh trắc học / điểm số chất lượng)
    val Tertiary = Color(0xFF005230)
    val TertiaryContainer = Color(0xFF006D41)
    val TertiaryFixed = Color(0xFF92F7BA)
    val OnTertiaryFixed = Color(0xFF002110)

    // Khối phụ trợ
    val SurfaceVariant = Color(0xFFF1F5F9)
    val SurfaceContainer = Color(0xFFEEF4FF)
    val SurfaceContainerHigh = Color(0xFFE0EBFF)
    val SurfaceContainerHighest = Color(0xFFC7D7FE)
    val SurfaceContainerLow = Color(0xFFF8FAFC)

    // Gradients cho Avatar chữ cái viết tắt (khớp 100% Stitch)
    val AvatarGradients = listOf(
        Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF4F46E5))), // Blue to Indigo
        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF9333EA))), // Indigo to Purple
        Brush.linearGradient(listOf(Color(0xFF1D4ED8), Color(0xFF0284C7))), // Blue to Sky
        Brush.linearGradient(listOf(Color(0xFF14B8A6), Color(0xFF059669))), // Teal to Emerald
        Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEA580C)))  // Amber to Orange
    )
}
