package com.rork.diariointimo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import kotlin.random.Random

private data class Speck(val x: Float, val y: Float, val r: Float, val alpha: Float)

private fun specks(count: Int, seed: Int): List<Speck> {
    val random = Random(seed)
    return List(count) {
        Speck(
            x = random.nextFloat(),
            y = random.nextFloat(),
            r = 0.4f + random.nextFloat() * 1.6f,
            alpha = 0.02f + random.nextFloat() * 0.07f
        )
    }
}

/** Fine paper grain, drawn once and cached. Follows the active diary palette. */
@Composable
fun Modifier.paperGrain(seed: Int = 7, count: Int = 260, tint: Color = Color.Unspecified): Modifier {
    val resolved = if (tint.isUnspecified) LocalDiaryColors.current.inkFaded else tint
    return this.drawWithCache {
        val dots = specks(count, seed)
        onDrawBehind {
            dots.forEach { dot ->
                drawCircle(
                    color = resolved.copy(alpha = dot.alpha),
                    radius = dot.r,
                    center = Offset(dot.x * size.width, dot.y * size.height)
                )
            }
        }
    }
}

/** The diary canvas: warm daylight paper, or deep ink-blue paper at night. */
@Composable
fun PaperBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalDiaryColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to colors.paperLight,
                    0.45f to colors.paper,
                    1f to colors.paperDeep
                )
            )
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, colors.inkFaded.copy(alpha = 0.16f)),
                        center = Offset(size.width * 0.5f, size.height * 0.42f),
                        radius = size.maxDimension * 0.78f
                    )
                )
            }
            .paperGrain(seed = 21, count = 320),
        content = content
    )
}

/** Candle-lit desk used behind the envelope on the lock screen. */
@Composable
fun DeskBackground(
    modifier: Modifier = Modifier,
    glow: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalDiaryColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to colors.deskMid,
                    0.5f to colors.deskWarm.copy(alpha = 0.95f),
                    1f to colors.deskDeep
                )
            )
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.goldLight.copy(alpha = 0.16f * glow),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.38f),
                        radius = size.minDimension * 1.05f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, colors.deskDeep.copy(alpha = 0.85f)),
                        center = Offset(size.width * 0.5f, size.height * 0.45f),
                        radius = size.maxDimension * 0.72f
                    )
                )
            }
            .paperGrain(seed = 3, count = 200, tint = Color.Black),
        content = content
    )
}

/** Faint ruled lines, like a real diary sheet. The color follows the palette. */
fun DrawScope.drawRuledLines(
    lineHeightPx: Float,
    color: Color,
    startY: Float = 0f
) {
    if (lineHeightPx <= 0f) return
    var y = startY + lineHeightPx
    while (y < size.height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += lineHeightPx
    }
}
