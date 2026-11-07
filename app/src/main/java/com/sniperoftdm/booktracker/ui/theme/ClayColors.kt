package com.sniperoftdm.booktracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

@Immutable
data class ClayColors(
    val face: Color,
    val highlight: Color,
    val shade: Color,
    val edge: Color,
    val accents: List<Color>,
    val sage: Color,
    val peach: Color,
    val butter: Color,
    val sky: Color,
    val lilac: Color,
    val terracotta: Color,
    val isDark: Boolean
) {
    fun textOnAccent(accent: Color): Color {
        // High contrast readable text on any clay accent or container face
        fun lin(c: Float): Double = if (c <= 0.04045f) (c / 12.92).toDouble() else Math.pow(((c + 0.055) / 1.055), 2.4)
        val lum = 0.2126 * lin(accent.red) + 0.7152 * lin(accent.green) + 0.0722 * lin(accent.blue)
        val lumDark = 0.00954
        val lumLight = 0.87739
        val contrastWithDark = (maxOf(lum, lumDark) + 0.05) / (minOf(lum, lumDark) + 0.05)
        val contrastWithLight = (maxOf(lum, lumLight) + 0.05) / (minOf(lum, lumLight) + 0.05)
        return if (contrastWithDark >= contrastWithLight) {
            Color(0xFF1C1815)
        } else {
            Color(0xFFF6F0E8)
        }
    }

    fun contentColorFor(surfaceColor: Color): Color {
        return when (surfaceColor) {
            face -> if (isDark) DarkInk else LightInk
            else -> textOnAccent(surfaceColor)
        }
    }

    fun accentForTitle(title: String): Color {
        if (accents.isEmpty()) return sage
        val hash = abs(title.trim().lowercase().hashCode())
        return accents[hash % accents.size]
    }
}

val LightClayColors = ClayColors(
    face = LightFace,
    highlight = Color.White.copy(alpha = 0.45f),
    shade = Color(0xFF3A2F2A).copy(alpha = 0.18f),
    edge = LightEdge,
    accents = listOf(LightSage, LightPeach, LightButter, LightSky, LightLilac, LightTerracotta),
    sage = LightSage,
    peach = LightPeach,
    butter = LightButter,
    sky = LightSky,
    lilac = LightLilac,
    terracotta = LightTerracotta,
    isDark = false
)

val DarkClayColors = ClayColors(
    face = DarkFace,
    highlight = Color.White.copy(alpha = 0.07f),
    shade = Color.Black.copy(alpha = 0.45f),
    edge = DarkEdge,
    accents = listOf(DarkSage, DarkPeach, DarkButter, DarkSky, DarkLilac, DarkTerracotta),
    sage = DarkSage,
    peach = DarkPeach,
    butter = DarkButter,
    sky = DarkSky,
    lilac = DarkLilac,
    terracotta = DarkTerracotta,
    isDark = true
)

val LocalClayColors = staticCompositionLocalOf { LightClayColors }
