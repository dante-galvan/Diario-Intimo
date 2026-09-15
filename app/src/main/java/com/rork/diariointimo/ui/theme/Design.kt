package com.rork.diariointimo.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The diary's single scale of measurements. Every screen and component draws
 * its spacing, radii, icon sizes and touch targets from here, so the whole
 * app breathes with the same rhythm on any device.
 */
object DiaryDim {
    // Spacing ladder: everything is a step on this scale.
    val space1: Dp = 4.dp
    val space2: Dp = 8.dp
    val space3: Dp = 12.dp
    val space4: Dp = 16.dp
    val space5: Dp = 20.dp
    val space6: Dp = 24.dp
    val space8: Dp = 32.dp
    val space10: Dp = 40.dp
    val space12: Dp = 48.dp
    val space16: Dp = 64.dp

    // Screen rhythm: outer margins and header rows are identical everywhere.
    val screenPad: Dp = 24.dp
    val headerPad: Dp = 16.dp

    // Corner radii: paper edges are barely rounded, like cut card stock.
    val radiusPaper: Dp = 2.dp
    val radiusSheet: Dp = 3.dp

    // Touch comfort: every interactive control is at least this large.
    val touchTarget: Dp = 48.dp

    // Icon sizes: one small, one medium, one large — nothing between.
    val iconSmall: Dp = 15.dp
    val iconMedium: Dp = 20.dp
    val iconLarge: Dp = 24.dp

    // Primary action height.
    val buttonHeight: Dp = 52.dp

    // Semantic dimensions with a fixed purpose across screens.
    val fabClearance: Dp = 90.dp
    val signatureHeight: Dp = 110.dp
    val sheetMinHeight: Dp = 380.dp
    val questionChipWidth: Dp = 128.dp
}
