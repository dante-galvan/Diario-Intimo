package com.rork.diariointimo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import kotlin.math.abs

/** The paper envelope: back panel, front pocket and a flap that really opens. */
@Composable
fun Envelope(
    flapAngle: Float,
    sealScale: Float,
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
    height: Dp = 178.dp
) {
    val colors = LocalDiaryColors.current
    Box(modifier = modifier.size(width, height)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Back panel
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(colors.paperDeep, colors.edge.copy(alpha = 0.92f))
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            // Inner shadow at the mouth
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.ink.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.35f
                ),
                size = Size(w, h * 0.35f)
            )

            // Front pocket
            val pocket = Path().apply {
                moveTo(0f, h * 0.26f)
                lineTo(w * 0.5f, h * 0.70f)
                lineTo(w, h * 0.26f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = pocket,
                brush = Brush.verticalGradient(
                    0f to colors.paper,
                    0.6f to colors.paperLight,
                    1f to colors.paperDeep
                )
            )
            drawPath(
                path = Path().apply {
                    moveTo(0f, h * 0.26f)
                    lineTo(w * 0.5f, h * 0.70f)
                    lineTo(w, h * 0.26f)
                },
                color = colors.gold.copy(alpha = 0.35f),
                style = Stroke(width = 1.4f)
            )
            drawLine(
                color = colors.ink.copy(alpha = 0.06f),
                start = Offset(0f, h * 0.995f),
                end = Offset(w, h * 0.995f),
                strokeWidth = 2f
            )
        }

        // Flap, hinged on the top edge
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(height * 0.70f)
                .graphicsLayer {
                    rotationX = flapAngle
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    cameraDistance = 16f * density
                }
        ) {
            val w = size.width
            val h = size.height
            val opened = abs(flapAngle) > 90f
            val triangle = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w * 0.5f, h)
                close()
            }
            drawPath(
                path = triangle,
                brush = if (opened) {
                    Brush.verticalGradient(listOf(colors.paperLight, colors.paper))
                } else {
                    Brush.verticalGradient(listOf(colors.paper, colors.paperDeep))
                }
            )
            drawPath(
                path = triangle,
                color = colors.gold.copy(alpha = 0.28f),
                style = Stroke(width = 1.2f)
            )
            // Fold shading while the flap travels
            val shade = (abs(flapAngle) / 180f).coerceIn(0f, 1f)
            drawPath(
                path = triangle,
                color = colors.ink.copy(alpha = 0.18f * (1f - abs(shade - 0.5f) * 2f))
            )

            if (sealScale > 0.01f) {
                val cx = w * 0.5f
                val cy = h * 0.86f
                val radius = w * 0.085f * sealScale
                drawCircle(
                    color = colors.waxDeep.copy(alpha = 0.35f),
                    radius = radius * 1.18f,
                    center = Offset(cx, cy + radius * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.wax, colors.waxDeep),
                        center = Offset(cx - radius * 0.3f, cy - radius * 0.3f),
                        radius = radius * 1.6f
                    ),
                    radius = radius,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = colors.goldLight.copy(alpha = 0.55f),
                    radius = radius * 0.62f,
                    center = Offset(cx, cy),
                    style = Stroke(width = radius * 0.09f)
                )
                // Tiny nib emblem
                drawPath(
                    path = Path().apply {
                        moveTo(cx, cy - radius * 0.42f)
                        lineTo(cx + radius * 0.2f, cy + radius * 0.1f)
                        lineTo(cx, cy + radius * 0.42f)
                        lineTo(cx - radius * 0.2f, cy + radius * 0.1f)
                        close()
                    },
                    color = colors.goldLight.copy(alpha = 0.85f)
                )
            }
        }
    }
}
