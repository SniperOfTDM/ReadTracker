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
        // High contrast readable text on any clay accent face
        return if (isDark) {
            Color(0xFF1C1815)
        } else {
            LightInk
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
