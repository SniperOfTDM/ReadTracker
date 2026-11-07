package com.sniperoftdm.booktracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sniperoftdm.booktracker.data.repository.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSage,
    onSecondaryContainer = LightInk,
    tertiary = LightTerracotta,
    onTertiary = Color.White,
    tertiaryContainer = LightPeach,
    onTertiaryContainer = LightInk,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightFace,
    onSurface = LightInk,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightSecondary,
    surfaceContainer = LightFace,
    surfaceContainerHigh = Color(0xFFFFFDF9),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    surfaceContainerLow = LightBackground,
    surfaceContainerLowest = Color(0xFFEDE4D8),
    outline = LightOutline,
    outlineVariant = LightEdge,
    error = LightError,
    onError = LightOnError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkFace,
    secondaryContainer = DarkSage,
    onSecondaryContainer = Color(0xFF1C1815),
    tertiary = DarkTerracotta,
    onTertiary = Color(0xFF1C1815),
    tertiaryContainer = DarkPeach,
    onTertiaryContainer = Color(0xFF1C1815),
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkFace,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF38302A),
    onSurfaceVariant = DarkSecondary,
    surfaceContainer = DarkFace,
    surfaceContainerHigh = Color(0xFF342C27),
    surfaceContainerHighest = Color(0xFF3D342E),
    surfaceContainerLow = DarkBackground,
    surfaceContainerLowest = Color(0xFF14110E),
    outline = DarkOutline,
    outlineVariant = DarkEdge,
    error = DarkError,
    onError = DarkOnError
)

@Composable
fun BookTrackerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamicColor && supportsDynamic -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val clayColors = when {
        useDynamicColor && supportsDynamic -> {
            if (darkTheme) {
                DarkClayColors.copy(
                    face = colorScheme.surfaceContainer,
                    sage = colorScheme.secondaryContainer,
                    terracotta = colorScheme.tertiaryContainer
                )
            } else {
                LightClayColors.copy(
                    face = colorScheme.surfaceContainer,
                    sage = colorScheme.secondaryContainer,
                    terracotta = colorScheme.tertiaryContainer
                )
            }
        }
        darkTheme -> DarkClayColors
        else -> LightClayColors
    }

    CompositionLocalProvider(
        LocalClayColors provides clayColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
