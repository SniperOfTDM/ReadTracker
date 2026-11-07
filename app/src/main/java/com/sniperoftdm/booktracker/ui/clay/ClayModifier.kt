package com.sniperoftdm.booktracker.ui.clay

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.Dimens

fun Modifier.clay(
    shape: Shape,
    color: Color,
    elevation: ClayElevation,
    pressed: Boolean = false,
    edgeColor: Color = Color.Unspecified
): Modifier = this.drawWithCache {
    val isInset = elevation == ClayElevation.Inset || pressed
    val outline = shape.createOutline(size, layoutDirection, this)

    val composePath = Path().apply {
        when (outline) {
            is Outline.Generic -> addPath(outline.path)
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Rectangle -> addRect(outline.rect)
        }
    }
    val androidPath = composePath.asAndroidPath()

    val shadowRadiusDp = when (elevation) {
        ClayElevation.Flat -> Dimens.shadowFlatRadius
        ClayElevation.Raised -> Dimens.shadowRaisedRadius
        ClayElevation.Floating -> Dimens.shadowFloatingRadius
        ClayElevation.Inset -> 0.dp
    }
    val shadowOffsetDp = when (elevation) {
        ClayElevation.Flat -> Dimens.shadowFlatOffset
        ClayElevation.Raised -> Dimens.shadowRaisedOffset
        ClayElevation.Floating -> Dimens.shadowFloatingOffset
        ClayElevation.Inset -> 0.dp
    }

    val shadowRadiusPx = shadowRadiusDp.toPx()
    val shadowOffsetPx = shadowOffsetDp.toPx()

    // Tinted drop shadow
    val shadowColor = Color(
        red = (color.red * 0.45f).coerceIn(0f, 1f),
        green = (color.green * 0.45f).coerceIn(0f, 1f),
        blue = (color.blue * 0.45f).coerceIn(0f, 1f),
        alpha = if (elevation == ClayElevation.Floating) 0.28f else 0.20f
    )

    val nativePaint = if (!isInset && shadowRadiusPx > 0f) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(shadowRadiusPx, 0f, shadowOffsetPx, shadowColor.toArgb())
        }
    } else null

    // 2. Body fill: subtle diagonal gradient
    val lighterFill = if (isInset) {
        color.copy(
            red = (color.red * 0.95f).coerceIn(0f, 1f),
            green = (color.green * 0.95f).coerceIn(0f, 1f),
            blue = (color.blue * 0.95f).coerceIn(0f, 1f)
        )
    } else {
        color.copy(
            red = (color.red * 1.05f).coerceIn(0f, 1f),
            green = (color.green * 1.05f).coerceIn(0f, 1f),
            blue = (color.blue * 1.05f).coerceIn(0f, 1f)
        )
    }

    val darkerFill = if (isInset) {
        color.copy(
            red = (color.red * 0.90f).coerceIn(0f, 1f),
            green = (color.green * 0.90f).coerceIn(0f, 1f),
            blue = (color.blue * 0.90f).coerceIn(0f, 1f)
        )
    } else {
        color.copy(
            red = (color.red * 0.95f).coerceIn(0f, 1f),
            green = (color.green * 0.95f).coerceIn(0f, 1f),
            blue = (color.blue * 0.95f).coerceIn(0f, 1f)
        )
    }

    val fillBrush = Brush.linearGradient(
        colors = listOf(lighterFill, color, darkerFill),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )

    // 3. Inner highlight and shade bevel
    val highlightAlpha = if (isInset) 0.35f else 0.45f
    val shadeAlpha = if (isInset) 0.25f else 0.18f

    val highlightColor = Color.White.copy(alpha = highlightAlpha)
    val shadeColor = Color.Black.copy(alpha = shadeAlpha)

    val innerBevelBrush = if (isInset) {
        Brush.linearGradient(
            colors = listOf(shadeColor, Color.Transparent, highlightColor),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(highlightColor, Color.Transparent, shadeColor),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    }

    // 4. Hairline edge
    val resolvedEdgeColor = if (edgeColor != Color.Unspecified) {
        edgeColor
    } else {
        color.copy(
            red = (color.red * 0.75f).coerceIn(0f, 1f),
            green = (color.green * 0.75f).coerceIn(0f, 1f),
            blue = (color.blue * 0.75f).coerceIn(0f, 1f),
            alpha = 0.35f
        )
    }
    val edgeBrush = SolidColor(resolvedEdgeColor)
    val innerStrokeWidth = Dimens.innerBevelWidth.toPx()
    val hairlineStrokeWidth = Dimens.hairlineEdgeWidth.toPx()

    onDrawWithContent {
        // Op 1: Outer drop shadow
        if (!isInset && nativePaint != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawPath(androidPath, nativePaint)
                }
            } else {
                // API 26-27 fallback: 2 stacked translucent offset paths
                val fallbackPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    this.color = shadowColor.copy(alpha = 0.10f).toArgb()
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(0f, shadowOffsetPx * 0.6f)
                    canvas.nativeCanvas.drawPath(androidPath, fallbackPaint)
                    canvas.nativeCanvas.translate(0f, shadowOffsetPx * 0.4f)
                    canvas.nativeCanvas.drawPath(androidPath, fallbackPaint)
                    canvas.nativeCanvas.restore()
                }
            }
        }

        // Op 2: Body Fill
        drawOutline(outline = outline, brush = fillBrush)

        // Op 3: Inner highlight & shade
        clipPath(composePath) {
            drawOutline(
                outline = outline,
                brush = innerBevelBrush,
                style = Stroke(width = innerStrokeWidth)
            )
        }

        // Op 4: Hairline Edge
        drawOutline(
            outline = outline,
            brush = edgeBrush,
            style = Stroke(width = hairlineStrokeWidth)
        )

        drawContent()
    }
}

fun Modifier.clayClickable(
    enabled: Boolean = true,
    shape: Shape,
    color: Color,
    elevation: ClayElevation = ClayElevation.Raised,
    role: Role? = null,
    onClickLabel: String? = null,
    isPrimaryAction: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "claySquish"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clay(
            shape = shape,
            color = color,
            elevation = elevation,
            pressed = isPressed && enabled
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = {
                if (isPrimaryAction) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
        )
}
