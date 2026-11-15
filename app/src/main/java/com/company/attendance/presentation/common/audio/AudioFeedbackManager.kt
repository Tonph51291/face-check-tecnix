package com.company.attendance.presentation.common.audio

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * AudioFeedbackManager: Quản lý âm thanh phản hồi tức thì cho Kiosk.
 * Tách biệt hoàn toàn logic phát âm thanh ra khỏi Composable UI.
 */
class AudioFeedbackManager {

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
    } catch (_: Exception) {
        null
    }

    /** Phát tiếng Beep vui tươi khi chấm công thành công */
    fun playSuccessBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {}
    }

    /** Phát tiếng cảnh báo khi chấm trùng lặp (Cooldown) */
    fun playWarningBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 160)
        } catch (_: Exception) {}
    }

    /** Phát tiếng báo lỗi khi khuôn mặt không khớp hoặc lỗi hệ thống */
    fun playErrorBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 160)
        } catch (_: Exception) {}
    }

    /** Giải phóng tài nguyên âm thanh */
    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
