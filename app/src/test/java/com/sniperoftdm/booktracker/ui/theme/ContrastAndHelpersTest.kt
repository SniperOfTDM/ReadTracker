package com.sniperoftdm.booktracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ContrastAndHelpersTest {

    private fun linearize(channel: Float): Double {
        return if (channel <= 0.04045f) {
            (channel / 12.92).toDouble()
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun relativeLuminance(color: Color): Double {
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun lightTheme_background_meetsWcagAA() {
        val ratio = contrastRatio(LightInk, LightBackground)
        assertTrue("LightInk on LightBackground ($ratio) must exceed 4.5:1", ratio >= 4.5)
    }

    @Test
    fun lightTheme_primaryButton_meetsWcagAA() {
        val ratio = contrastRatio(LightOnPrimary, LightPrimary)
        assertTrue("White on LightPrimary ($ratio) must exceed 4.5:1", ratio >= 4.5)
    }

    @Test
    fun lightTheme_accents_withInk_meetWcagAA() {
        val accents = listOf(LightSage, LightPeach, LightButter, LightSky, LightLilac, LightTerracotta)
        for (accent in accents) {
            val ratio = contrastRatio(LightInk, accent)
            assertTrue("LightInk on accent ($ratio) must exceed 4.5:1", ratio >= 4.5)
        }
    }

    @Test
    fun darkTheme_background_meetsWcagAA() {
        val ratio = contrastRatio(DarkInk, DarkBackground)
        assertTrue("DarkInk on DarkBackground ($ratio) must exceed 4.5:1", ratio >= 4.5)
    }

    @Test
    fun darkTheme_face_meetsWcagAA() {
        val ratio = contrastRatio(DarkInk, DarkFace)
        assertTrue("DarkInk on DarkFace ($ratio) must exceed 4.5:1", ratio >= 4.5)
    }

    @Test
    fun darkTheme_accents_withTextOnAccent_meetWcagAA() {
        val accents = listOf(DarkSage, DarkPeach, DarkButter, DarkSky, DarkLilac, DarkTerracotta)
        for (accent in accents) {
            val textColor = DarkClayColors.textOnAccent(accent)
            val ratio = contrastRatio(textColor, accent)
            assertTrue("Text on dark mode accent ($ratio) must exceed 4.5:1", ratio >= 4.5)
        }
    }

    @Test
    fun accentForTitle_isDeterministicAndStable() {
        val title = "The Lord of the Rings"
        val accent1 = LightClayColors.accentForTitle(title)
        val accent2 = LightClayColors.accentForTitle(title)
        val accentCase = LightClayColors.accentForTitle("the lord of the rings")

        assertEquals(accent1, accent2)
        assertEquals(accent1, accentCase)
    }
}
