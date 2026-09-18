package com.rork.diariointimo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import kotlin.math.min
import kotlin.random.Random

/**
 * Builds a cursive-looking stroke for [count] characters. The shape is
 * deterministic for a given seed so the writing never jumps while typing.
 */
private fun scriptPath(count: Int, unit: Float, baseline: Float, seed: Int): Path {
    val path = Path()
    if (count <= 0) return path
    val random = Random(seed)
    path.moveTo(0f, baseline)
    var x = 0f
    repeat(count) { index ->
        val up = baseline - unit * (0.55f + random.nextFloat() * 0.55f)
        val dip = baseline + unit * (random.nextFloat() * 0.22f)
        val loop = random.nextFloat() > 0.62f
        if (loop) {
            path.cubicTo(
                x + unit * 0.10f, up - unit * 0.35f,
                x + unit * 0.72f, up - unit * 0.30f,
                x + unit * 0.52f, baseline
            )
            path.cubicTo(
                x + unit * 0.40f, dip + unit * 0.28f,
                x + unit * 0.92f, dip + unit * 0.18f,
                x + unit, baseline
            )
        } else {
            path.cubicTo(
                x + unit * 0.18f, up,
                x + unit * 0.55f, up + unit * 0.12f,
                x + unit * 0.62f, baseline - unit * 0.06f
            )
            path.cubicTo(
                x + unit * 0.72f, dip,
                x + unit * 0.88f, dip,
                x + unit, baseline
            )
        }
        x += unit
        if (index % 4 == 3) {
            path.moveTo(x + unit * 0.06f, baseline)
        }
    }
    return path
}

/**
 * Ink handwriting that grows with the number of typed characters, with the
 * fountain pen nib riding at the tip of the stroke.
 */
@Composable
fun HandwrittenSecret(
    characterCount: Int,
    modifier: Modifier = Modifier,
    lineHeight: Dp = 54.dp,
    inkColor: Color = Color.Unspecified,
    penHeight: Dp = 92.dp,
    showPen: Boolean = true
) {
    val ink = if (inkColor.isUnspecified) LocalDiaryColors.current.inkBlue else inkColor
    BoxWithConstraints(modifier = modifier.height(lineHeight)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val unit = with(density) { 15.dp.toPx() }
        val maxChars = ((widthPx - with(density) { 24.dp.toPx() }) / unit).toInt().coerceAtLeast(1)
        val visibleChars = min(characterCount, maxChars)

        val progress = remember { Animatable(0f) }
        val target by rememberUpdatedState(visibleChars.toFloat())
        LaunchedEffect(visibleChars) {
            progress.animateTo(target, animationSpec = tween(durationMillis = 190))
        }

        val baseline = with(density) { (lineHeight * 0.72f).toPx() }
        val fullPath = remember(visibleChars) {
            scriptPath(visibleChars, unit, baseline, seed = 1979)
        }
        val strokeWidth = with(density) { 2.4.dp.toPx() }

        // The pen rides behind the ink: it accompanies the writing, never covers it.
        if (showPen && visibleChars > 0) {
            val penX = with(density) { (progress.value * unit + unit * 0.6f).toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = penX - penHeight * 0.17f, y = lineHeight * 0.72f - penHeight)
            ) {
                FountainPen(height = penHeight, tilt = 18f, writing = true)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (visibleChars == 0) return@Canvas
            val fraction = (progress.value / visibleChars.toFloat()).coerceIn(0f, 1f)
            val measure = PathMeasure().apply { setPath(fullPath, false) }
            val visible = Path()
            measure.getSegment(0f, measure.length * fraction, visible, true)
            // Ink bleed underneath, then the crisp stroke on top.
            drawPath(
                path = visible,
                color = ink.copy(alpha = 0.16f),
                style = Stroke(
                    width = strokeWidth * 2.1f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawPath(
                path = visible,
                color = ink.copy(alpha = 0.92f),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            // Fresh ink droplet at the nib.
            val tipX = progress.value * unit
            drawCircle(
                color = ink.copy(alpha = 0.35f),
                radius = strokeWidth * 1.15f,
                center = Offset(tipX, baseline)
            )
        }
    }
}

/**
 * Security dots that grow with the number of typed characters, with the
 * fountain pen nib riding after the last dot. The secret is never shown.
 */
@Composable
fun SecretDots(
    characterCount: Int,
    modifier: Modifier = Modifier,
    lineHeight: Dp = 54.dp,
    inkColor: Color = Color.Unspecified,
    penHeight: Dp = 92.dp,
    showPen: Boolean = true
) {
    val ink = if (inkColor.isUnspecified) LocalDiaryColors.current.inkBlue else inkColor
    BoxWithConstraints(modifier = modifier.height(lineHeight)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val unit = with(density) { 15.dp.toPx() }
        val maxChars = ((widthPx - with(density) { 24.dp.toPx() }) / unit).toInt().coerceAtLeast(1)
        val visibleChars = min(characterCount, maxChars)

        val progress = remember { Animatable(0f) }
        val target by rememberUpdatedState(visibleChars.toFloat())
        LaunchedEffect(visibleChars) {
            progress.animateTo(target, animationSpec = tween(durationMillis = 190))
        }

        // Baseline aligned to match BasicTextField centered text position:
        // containerHeight/2 + fontSize/3 for a 13sp font in a 48dp field ≈ 0.58 * height
        val baseline = with(density) { (lineHeight * 0.58f).toPx() }

        // The pen rides behind the dots: it accompanies the writing, never covers it.
        if (showPen && visibleChars > 0) {
            val penX = with(density) { (progress.value * unit + unit * 0.6f).toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = penX - penHeight * 0.17f, y = lineHeight * 0.58f - penHeight)
            ) {
                FountainPen(height = penHeight, tilt = 18f, writing = true)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (visibleChars == 0) return@Canvas
            val dotRadius = 3.4.dp.toPx()
            repeat(visibleChars) { index ->
                val appear = (progress.value - index).coerceIn(0f, 1f)
                if (appear <= 0f) return@repeat
                val cx = (index + 0.5f) * unit
                val scale = if (index == visibleChars - 1) 0.55f + 0.45f * appear else 1f
                // Ink bleed underneath, then the crisp dot on top.
                drawCircle(
                    color = ink.copy(alpha = 0.16f),
                    radius = dotRadius * scale * 1.9f,
                    center = Offset(cx, baseline)
                )
                drawCircle(
                    color = ink.copy(alpha = 0.9f),
                    radius = dotRadius * scale,
                    center = Offset(cx, baseline)
                )
            }
        }
    }
}

/** The thin ruled line the secret is written on. */
@Composable
fun InkRule(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val ink = if (color.isUnspecified) {
        LocalDiaryColors.current.inkFaded.copy(alpha = 0.45f)
    } else {
        color
    }
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = ink,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height
        )
    }
}
