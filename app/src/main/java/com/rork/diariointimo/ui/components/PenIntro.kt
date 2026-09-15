package com.rork.diariointimo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import com.rork.diariointimo.ui.theme.SerifFamily
import kotlinx.coroutines.delay

/**
 * The opening scene: on a candle-lit desk, the fountain pen signs the diary
 * open. Brief, cinematic, and skippable with a tap.
 */
@Composable
fun PenIntro(
    title: String,
    tagline: String,
    onFinished: () -> Unit
) {
    val colors = LocalDiaryColors.current
    val reveal = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(1500, easing = EaseInOutCubic))
        delay(420)
        fade.animateTo(0f, tween(520))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onFinished() }
            .graphicsLayer { alpha = fade.value }
    ) {
        DeskBackground(glow = reveal.value) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FountainPen(height = 132.dp, tilt = -16f, writing = true)
            Spacer(Modifier.height(34.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = SerifFamily),
                color = colors.paperLight,
                textAlign = TextAlign.Center,
                // The name appears as if the pen had just written it.
                modifier = Modifier.drawWithContent {
                    clipRect(right = size.width * reveal.value) { this@drawWithContent.drawContent() }
                }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = tagline,
                style = MaterialTheme.typography.labelMedium,
                color = colors.goldLight.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
        }
    }
}
