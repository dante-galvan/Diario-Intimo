package com.rork.diariointimo.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The complete palette of the diary. Two curated sets exist: the warm daylight
 * paper and the «Noche de tinta» dark set — a deep ink-blue night designed for
 * this identity, never an inverted copy of the light theme.
 */
@Immutable
data class DiaryColors(
    val paper: Color,
    val paperLight: Color,
    val paperDeep: Color,
    val edge: Color,
    val shadow: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaded: Color,
    val inkWhisper: Color,
    val inkBlue: Color,
    val gold: Color,
    val goldLight: Color,
    val wax: Color,
    val waxDeep: Color,
    val deskDeep: Color,
    val deskMid: Color,
    val deskWarm: Color,
    val deskCandle: Color,
    val isDark: Boolean
)

/** Warm, nostalgic daylight palette: aged paper, ink and candle light. */
fun lightDiaryColors(): DiaryColors = DiaryColors(
    paper = Color(0xFFF1E4CD),
    paperLight = Color(0xFFFBF4E6),
    paperDeep = Color(0xFFE5D5B7),
    edge = Color(0xFFD3BF9C),
    shadow = Color(0x332A1C10),
    ink = Color(0xFF241C15),
    inkSoft = Color(0xFF3E3227),
    inkFaded = Color(0xFF6B5B49),
    inkWhisper = Color(0xFF9C8A72),
    inkBlue = Color(0xFF1F2A3D),
    gold = Color(0xFFB08A4A),
    goldLight = Color(0xFFD8B978),
    wax = Color(0xFF7A3B34),
    waxDeep = Color(0xFF5A2723),
    deskDeep = Color(0xFF120C07),
    deskMid = Color(0xFF2A1C11),
    deskWarm = Color(0xFF3E2A19),
    deskCandle = Color(0x33E4B96A),
    isDark = false
)

/** «Noche de tinta»: deep ink-blue paper under lamplight, marfil writing, brass details. */
fun darkDiaryColors(): DiaryColors = DiaryColors(
    paper = Color(0xFF10151C),
    paperLight = Color(0xFF1A222C),
    paperDeep = Color(0xFF0B0F16),
    edge = Color(0xFF2C3644),
    shadow = Color(0x66050810),
    ink = Color(0xFFE8E2D4),
    inkSoft = Color(0xFFCFC8B8),
    inkFaded = Color(0xFF9C968A),
    inkWhisper = Color(0xFF6F6B62),
    inkBlue = Color(0xFFA9C3DC),
    gold = Color(0xFFC9A86A),
    goldLight = Color(0xFFE4CB92),
    wax = Color(0xFFC4635A),
    waxDeep = Color(0xFF8E4038),
    deskDeep = Color(0xFF06080C),
    deskMid = Color(0xFF121924),
    deskWarm = Color(0xFF18202C),
    deskCandle = Color(0x33C9A86A),
    isDark = true
)

/** Provided once in [com.rork.diariointimo.ui.theme.AppTheme] and read everywhere. */
val LocalDiaryColors = staticCompositionLocalOf { lightDiaryColors() }
