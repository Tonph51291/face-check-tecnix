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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.attendance.TrackFaceStatus
import com.company.attendance.presentation.attendance.TrackedFaceUiModel

/**
 * Lớp phủ vẽ khung ô vuông 4 góc công nghệ cao (Corner HUD Brackets ┌ ┐ └ ┘) và thẻ tên AR bay theo từng người.
 * Hỗ trợ nhận diện đa mục tiêu Walk-Through đồng thời nhiều người trong cùng 1 khung hình.
 */
@Composable
fun FaceDetectionOverlay(
    faces: List<Rect>,
    imageWidth: Int,
    imageHeight: Int,
    primaryFaceBox: Rect? = null,
    isFrontCamera: Boolean = false,
    isMultipleFaces: Boolean = false,
    trackedFaces: List<TrackedFaceUiModel> = emptyList(),
    modifier: Modifier = Modifier
) {
    val itemsToDraw = if (trackedFaces.isNotEmpty()) {
        trackedFaces
    } else {
        faces.map { rect ->
            TrackedFaceUiModel(
                trackingId = rect.hashCode(),
                boundingBox = rect,
                status = TrackFaceStatus.DETECTED
            )
        }
    }

    if (itemsToDraw.isEmpty() || imageWidth <= 0 || imageHeight <= 0) {
        return
    }

    val density = LocalDensity.current
    val bracketStrokeWidth = remember(density) { with(density) { 3.dp.toPx() } }
    val bracketLength = remember(density) { with(density) { 24.dp.toPx() } }
    val badgeBorderStroke = remember { androidx.compose.ui.graphics.drawscope.Stroke(width = 2f) }

    // Text Paint tái sử dụng để vẽ thẻ tên AR mà không sinh rác trên Heap
    val textPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 13.sp.toPx() }
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val scale = maxOf(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        val offsetX = (canvasWidth - scaledWidth) / 2f
        val offsetY = (canvasHeight - scaledHeight) / 2f

        itemsToDraw.forEach { item ->
            val faceRect = item.boundingBox

            // Màu sắc và thẻ tên theo trạng thái của từng người
            val (themeColor, badgeText, badgeBgColor) = when (item.status) {
                TrackFaceStatus.SUCCESS, TrackFaceStatus.COOLDOWN -> {
                    val name = if (!item.employeeName.isNullOrBlank()) "✓ ${item.employeeName}" else "✓ Chấm công thành công"
                    Triple(Color(0xFF10B981), name, Color(0xEE065F46))
                }
                TrackFaceStatus.VERIFYING -> {
                    Triple(Color(0xFF00E5FF), "Đang nhận diện...", Color(0xEE0E7490))
                }
                TrackFaceStatus.DUPLICATE -> {
                    Triple(Color(0xFFF59E0B), "✓ Đã ghi nhận", Color(0xEEB45309))
                }
                TrackFaceStatus.FAILURE -> {
                    val msg = item.message ?: "Chưa nhận diện"
                    Triple(Color(0xFFEF4444), msg, Color(0xEE991B1B))
                }
                TrackFaceStatus.DETECTED, TrackFaceStatus.SEARCHING -> {
                    Triple(Color(0xFFF8FAFC), null, Color.Transparent)
                }
            }

            // Chuyển đổi tọa độ khuôn mặt sang màn hình
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
            val renderLeft = minOf(rawLeft, rawRight)
            val renderRight = maxOf(rawLeft, rawRight)

            if (renderRight <= 0f || renderLeft >= canvasWidth || targetBottom <= 0f || targetTop >= canvasHeight) {
                return@forEach
            }

            val safePadding = 10f
            val drawLeft = renderLeft.coerceIn(safePadding, canvasWidth - safePadding)
            val drawTop = targetTop.coerceIn(safePadding, canvasHeight - safePadding)
            val drawRight = renderRight.coerceIn(safePadding, canvasWidth - safePadding)
            val drawBottom = targetBottom.coerceIn(safePadding, canvasHeight - safePadding)

            val boxWidth = drawRight - drawLeft
            val boxHeight = drawBottom - drawTop
            if (boxWidth < 30f || boxHeight < 30f) return@forEach

            // 1. Vẽ 4 góc ngắm Sleek Corner Reticle ┌ ┐ └ ┘
            val actualBracketLen = minOf(bracketLength, boxWidth * 0.25f, boxHeight * 0.25f)

            // Góc trên - trái: ┌
            drawLine(color = themeColor, start = Offset(drawLeft, drawTop + actualBracketLen), end = Offset(drawLeft, drawTop), strokeWidth = bracketStrokeWidth, cap = StrokeCap.Round)
            drawLine(color = themeColor, start = Offset(drawLeft, drawTop), end = Offset(drawLeft + actualBracketLen, drawTop), strokeWidth = bracketStrokeWidth, cap = StrokeCap.Round)

            // Góc trên - phải: ┐
            drawLine(color = themeColor, start = Offset(drawRight - actualBracketLen, drawTop), end = Offset(drawRight, drawTop), strokeWidth = bracketStrokeWidth, cap = StrokeCap.Round)
            drawLine(color = themeColor, start = Offset(drawRight, drawTop), end = Offset(drawRight, drawTop + actualBracketLen), strokeWidth = bracketStrokeWidth, cap = StrokeCap.Round)

            // Góc dưới - trái: └
            drawLine(color = themeColor, start = Offset(drawLeft, drawBottom - actualBracketLen), end = Offset(drawLeft, drawBottom), strokeWidth = bracketStrokeWidth, cap = StrokeCap.Round)
            drawLine(color = themeColor, start = Offset(drawLeft, drawBottom), end = Offset(drawLeft + actualBracketLen, drawBottom), strokeWidth = bracketStrokeWidth, cap = StrokeCap.Round)

            // Góc dưới - phải: ┘
            drawLine(color = themeColor, start = Offset(drawRight - actualBracketLen, drawBottom), end = Offset(drawRight, drawBottom), strokeWidth = bracketStrokeWidth, cap = StrokeCap.Round)
            drawLine(color = themeColor, start = Offset(drawRight, drawBottom), end = Offset(drawRight, drawBottom - actualBracketLen), strokeWidth = bracketStrokeWidth, cap = StrokeCap.Round)

            // 2. Vẽ thẻ tên AR bay trên đầu (Floating Name Tag Badge)
            if (badgeText != null) {
                val textWidth = textPaint.measureText(badgeText)
                val pillPaddingX = 24f
                val pillHeight = 44f
                val pillWidth = textWidth + (pillPaddingX * 2)
                val pillLeft = (drawLeft + (boxWidth - pillWidth) / 2f).coerceIn(12f, canvasWidth - pillWidth - 12f)
                val pillTop = (drawTop - pillHeight - 14f).coerceAtLeast(12f)

                // Nền thẻ bo tròn
                drawRoundRect(
                    color = badgeBgColor,
                    topLeft = Offset(pillLeft, pillTop),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )

                // Viền mỏng xung quanh thẻ
                drawRoundRect(
                    color = themeColor.copy(alpha = 0.8f),
                    topLeft = Offset(pillLeft, pillTop),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = badgeBorderStroke
                )

                // Chữ tên nhân viên
                drawContext.canvas.nativeCanvas.drawText(
                    badgeText,
                    pillLeft + pillPaddingX,
                    pillTop + pillHeight * 0.68f,
                    textPaint
                )
            }
        }
    }
}
