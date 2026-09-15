package com.rork.diariointimo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import com.rork.diariointimo.ui.theme.DiaryDim
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import com.rork.diariointimo.ui.theme.SansFamily

/** Primary action: deep ink with a thin gold rule and a soft press. */
@Composable
fun SealedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = LocalDiaryColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 620f),
        label = "buttonScale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = Color.Transparent,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else 0.5f
        }
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(listOf(colors.inkSoft, colors.ink))
                )
                .border(
                    BorderStroke(1.dp, colors.gold.copy(alpha = 0.55f)),
                    RoundedCornerShape(DiaryDim.radiusPaper)
                )
                .defaultMinSize(minHeight = DiaryDim.buttonHeight)
                .padding(horizontal = DiaryDim.space6, vertical = DiaryDim.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.goldLight,
                    modifier = Modifier.size(DiaryDim.iconSmall + 2.dp)
                )
                Spacer(Modifier.width(DiaryDim.space3))
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                ),
                color = colors.paperLight,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Quiet circular control used in headers. */
@Composable
fun InkIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    active: Boolean = false,
    enabled: Boolean = true
) {
    val colors = LocalDiaryColors.current
    val resolvedTint = if (tint.isUnspecified) colors.inkSoft else tint
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "iconScale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = CircleShape,
        color = if (active) colors.gold.copy(alpha = 0.16f) else colors.paperLight.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, colors.edge.copy(alpha = if (active) 0.9f else 0.55f)),
        modifier = modifier
            .semantics {
                this.contentDescription = contentDescription
            }
            .size(DiaryDim.touchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) colors.gold else resolvedTint,
                modifier = Modifier.size(DiaryDim.iconMedium)
            )
        }
    }
}

/** Small paper tab used for filters. */
@Composable
fun PaperChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDiaryColors.current
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(1.dp),
        color = if (selected) colors.ink else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) colors.gold.copy(alpha = 0.6f) else colors.edge
        ),
        modifier = modifier.semantics {
            stateDescription = if (selected) "selected" else "not selected"
            role = Role.Tab
        }
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.paperLight else colors.inkFaded,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = DiaryDim.space3, vertical = DiaryDim.space2)
        )
    }
}
