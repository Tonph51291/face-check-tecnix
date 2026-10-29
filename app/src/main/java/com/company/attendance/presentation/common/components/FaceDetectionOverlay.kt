package com.company.attendance.presentation.common.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lớp phủ vẽ khung ô vuông 4 góc công nghệ cao (Corner HUD Brackets ┌ ┐ └ ┘) bám sát khuôn mặt.
 * - Chỉ hiển thị khi có khuôn mặt trong khung hình.
 * - Hỗ trợ nhiều người cùng lúc: Phân biệt người đang quét chính (Primary) và người đang chờ.
 * - Xử lý chuẩn xác tỉ lệ phóng to FILL_CENTER của PreviewView và lật gương cho Camera trước.
 */
@Composable
fun FaceDetectionOverlay(
    faces: List<Rect>,
    imageWidth: Int,
    imageHeight: Int,
    primaryFaceBox: Rect? = null,
    isFrontCamera: Boolean = false,
    isMultipleFaces: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Chỉ hiển thị khung khi có khuôn mặt trong khung hình
    if (faces.isEmpty() || imageWidth <= 0 || imageHeight <= 0) {
        return
    }

    val density = LocalDensity.current
    val bracketStrokeWidth = with(density) { 3.dp.toPx() }
    val bracketLength = with(density) { 24.dp.toPx() }

    // Lưu tọa độ frame trước để áp dụng LERP làm mượt 60 FPS
    val animBounds = remember { FloatArray(4) { -1f } }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Tính toán tỉ lệ Scale theo ScaleType.FILL_CENTER
        val scale = maxOf(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        val offsetX = (canvasWidth - scaledWidth) / 2f
        val offsetY = (canvasHeight - scaledHeight) / 2f

        faces.forEachIndexed { index, faceRect ->
            // Xác định xem khuôn mặt này có phải là người đang được ưu tiên quét hay không
            val isPrimary = (primaryFaceBox == null && index == 0) || (primaryFaceBox != null && primaryFaceBox == faceRect)

            // Màu sắc theo trạng thái:
            // Người chính: Xanh Neon / Emerald nổi bật
            // Người phụ / đang chờ: Màu Vàng Hổ Phách
            val themeColor = if (isPrimary) Color(0xFF00E676) else Color(0xFFFBBF24)

            // 2. Chuyển đổi tọa độ khuôn mặt sang hệ quy chiếu màn hình Compose
            val rawLeft = if (isFrontCamera) {
                canvasWidth - (faceRect.right * scale + offsetX)
            } else {
                faceRect.left * scale + offsetX
            }

            val rawRight = if (isFrontCamera) {
                canvasWidth - (faceRect.left * scale + offsetX)
            } else {
                faceRect.right * scale + offsetX
            }

            val targetTop = faceRect.top * scale + offsetY
            val targetBottom = faceRect.bottom * scale + offsetY
            val targetLeft = minOf(rawLeft, rawRight)
            val targetRight = maxOf(rawLeft, rawRight)

            // 3. Thuật toán LERP (Linear Interpolation) làm mượt 60 FPS bám sát chuyển động mặt
            val renderLeft: Float
            val renderTop: Float
            val renderRight: Float
            val renderBottom: Float

            if (isPrimary) {
                val prevLeft = animBounds[0]
                val prevTop = animBounds[1]
                val prevRight = animBounds[2]
                val prevBottom = animBounds[3]

                if (prevLeft < 0f || kotlin.math.abs(targetLeft - prevLeft) > 200f) {
                    renderLeft = targetLeft
                    renderTop = targetTop
                    renderRight = targetRight
                    renderBottom = targetBottom
                } else {
                    renderLeft = prevLeft + (targetLeft - prevLeft) * 0.70f
                    renderTop = prevTop + (targetTop - prevTop) * 0.70f
                    renderRight = prevRight + (targetRight - prevRight) * 0.70f
                    renderBottom = prevBottom + (targetBottom - prevBottom) * 0.70f
                }
                animBounds[0] = renderLeft
                animBounds[1] = renderTop
                animBounds[2] = renderRight
                animBounds[3] = renderBottom
            } else {
                renderLeft = targetLeft
                renderTop = targetTop
                renderRight = targetRight
                renderBottom = targetBottom
            }

            // Kiểm tra biên an toàn: Nếu khuôn mặt nằm hoàn toàn ngoài màn hình thì bỏ qua
            if (renderRight <= 0f || renderLeft >= canvasWidth || renderBottom <= 0f || renderTop >= canvasHeight) {
                return@forEachIndexed
            }

            // Kẹp tọa độ hiển thị vào bên trong màn hình để 4 góc không bao giờ bị cắt cụt (Glitch mất góc)
            val safePadding = 10f
            val drawLeft = renderLeft.coerceIn(safePadding, canvasWidth - safePadding)
            val drawTop = renderTop.coerceIn(safePadding, canvasHeight - safePadding)
            val drawRight = renderRight.coerceIn(safePadding, canvasWidth - safePadding)
            val drawBottom = renderBottom.coerceIn(safePadding, canvasHeight - safePadding)

            val boxWidth = drawRight - drawLeft
            val boxHeight = drawBottom - drawTop

            if (boxWidth < 30f || boxHeight < 30f) return@forEachIndexed

            // Vẽ 4 góc ngắm tinh tế (Sleek Corner Reticle ┌ ┐ └ ┘) không che mặt người dùng
            val actualBracketLen = minOf(bracketLength, boxWidth * 0.25f, boxHeight * 0.25f)
            val bracketStroke = Stroke(width = bracketStrokeWidth, cap = StrokeCap.Round)

            // Góc trên - trái: ┌
            val pathTL = Path().apply {
                moveTo(drawLeft, drawTop + actualBracketLen)
                lineTo(drawLeft, drawTop)
                lineTo(drawLeft + actualBracketLen, drawTop)
            }
            drawPath(pathTL, color = themeColor, style = bracketStroke)

            // Góc trên - phải: ┐
            val pathTR = Path().apply {
                moveTo(drawRight - actualBracketLen, drawTop)
                lineTo(drawRight, drawTop)
                lineTo(drawRight, drawTop + actualBracketLen)
            }
            drawPath(pathTR, color = themeColor, style = bracketStroke)

            // Góc dưới - trái: └
            val pathBL = Path().apply {
                moveTo(drawLeft, drawBottom - actualBracketLen)
                lineTo(drawLeft, drawBottom)
                lineTo(drawLeft + actualBracketLen, drawBottom)
            }
            drawPath(pathBL, color = themeColor, style = bracketStroke)

            // Góc dưới - phải: ┘
            val pathBR = Path().apply {
                moveTo(drawRight - actualBracketLen, drawBottom)
                lineTo(drawRight, drawBottom)
                lineTo(drawRight, drawBottom - actualBracketLen)
            }
            drawPath(pathBR, color = themeColor, style = bracketStroke)
        }
    }
}
