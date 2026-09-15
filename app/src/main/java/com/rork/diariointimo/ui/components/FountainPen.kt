package com.rork.diariointimo.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rork.diariointimo.ui.theme.DiaryColors
import com.rork.diariointimo.ui.theme.LocalDiaryColors

/**
 * A fountain pen drawn entirely with vectors: lacquered barrel, gold band and nib.
 * The nib sits at the bottom-centre of the canvas so it can be anchored to a caret.
 */
@Composable
fun FountainPen(
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    tilt: Float = -22f,
    writing: Boolean = false
) {
    val colors = LocalDiaryColors.current
    val transition = rememberInfiniteTransition(label = "pen")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (writing) 1f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (writing) 460 else 2600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    Canvas(
        modifier = modifier
            .size(width = height * 0.34f, height = height)
            .graphicsLayer {
                rotationZ = tilt + bob * (if (writing) 2.4f else 0.9f)
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                translationY = -bob * (if (writing) 1.6f else 0.8f)
            }
    ) {
        drawFountainPen(colors)
    }
}

private fun DrawScope.drawFountainPen(colors: DiaryColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val barrelWidth = w * 0.5f

    // Soft shadow under the pen
    drawOval(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(cx - w * 0.24f, h * 0.94f),
        size = androidx.compose.ui.geometry.Size(w * 0.48f, h * 0.05f)
    )

    // Barrel
    val barrel = Path().apply {
        moveTo(cx - barrelWidth / 2f, h * 0.06f)
        lineTo(cx + barrelWidth / 2f, h * 0.06f)
        lineTo(cx + barrelWidth * 0.46f, h * 0.60f)
        lineTo(cx - barrelWidth * 0.46f, h * 0.60f)
        close()
    }
    drawPath(
        path = barrel,
        brush = Brush.horizontalGradient(
            0f to Color(0xFF14100C),
            0.35f to Color(0xFF3A2C20),
            0.62f to Color(0xFF241B13),
            1f to Color(0xFF100C09),
            startX = cx - barrelWidth / 2f,
            endX = cx + barrelWidth / 2f
        )
    )
    // Cap top ring
    drawRect(
        color = colors.gold,
        topLeft = Offset(cx - barrelWidth / 2f, h * 0.06f),
        size = androidx.compose.ui.geometry.Size(barrelWidth, h * 0.018f)
    )
    // Highlight
    drawLine(
        color = Color.White.copy(alpha = 0.16f),
        start = Offset(cx - barrelWidth * 0.22f, h * 0.09f),
        end = Offset(cx - barrelWidth * 0.2f, h * 0.57f),
        strokeWidth = w * 0.05f
    )
    // Gold band
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(colors.gold, colors.goldLight, colors.gold)
        ),
        topLeft = Offset(cx - barrelWidth * 0.48f, h * 0.60f),
        size = androidx.compose.ui.geometry.Size(barrelWidth * 0.96f, h * 0.055f)
    )

    // Grip section
    val grip = Path().apply {
        moveTo(cx - barrelWidth * 0.44f, h * 0.655f)
        lineTo(cx + barrelWidth * 0.44f, h * 0.655f)
        lineTo(cx + barrelWidth * 0.30f, h * 0.775f)
        lineTo(cx - barrelWidth * 0.30f, h * 0.775f)
        close()
    }
    drawPath(grip, color = Color(0xFF1B1410))

    // Nib
    val nib = Path().apply {
        moveTo(cx - barrelWidth * 0.30f, h * 0.775f)
        lineTo(cx + barrelWidth * 0.30f, h * 0.775f)
        lineTo(cx + barrelWidth * 0.06f, h * 0.985f)
        lineTo(cx, h)
        lineTo(cx - barrelWidth * 0.06f, h * 0.985f)
        close()
    }
    drawPath(
        path = nib,
        brush = Brush.verticalGradient(
            0f to colors.goldLight,
            0.55f to colors.gold,
            1f to Color(0xFF8A6A32),
            startY = h * 0.775f,
            endY = h
        )
    )
    drawLine(
        color = Color(0xFF6E5326),
        start = Offset(cx, h * 0.83f),
        end = Offset(cx, h),
        strokeWidth = w * 0.035f
    )
    drawCircle(
        color = Color(0xFF6E5326),
        radius = w * 0.055f,
        center = Offset(cx, h * 0.825f)
    )
}

/** Convenience rect describing where a caret sits, used to anchor the pen. */
data class CaretAnchor(val rect: Rect, val visible: Boolean)
