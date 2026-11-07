package com.sniperoftdm.booktracker.ui.clay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LightInk
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

enum class ClayButtonVariant {
    Filled,
    Tonal,
    OutlinedInset
}

@Composable
fun ClayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ClayButtonVariant = ClayButtonVariant.Filled,
    shape: Shape = ClayShapes.button,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clayColors = LocalClayColors.current

    val surfaceColor = when (variant) {
        ClayButtonVariant.Filled -> MaterialTheme.colorScheme.primary
        ClayButtonVariant.Tonal -> clayColors.sage
        ClayButtonVariant.OutlinedInset -> clayColors.face
    }

    val contentColor = when (variant) {
        ClayButtonVariant.Filled -> MaterialTheme.colorScheme.onPrimary
        ClayButtonVariant.Tonal -> clayColors.textOnAccent(clayColors.sage)
        ClayButtonVariant.OutlinedInset -> clayColors.contentColorFor(clayColors.face)
    }

    val elevation = when (variant) {
        ClayButtonVariant.Filled -> ClayElevation.Raised
        ClayButtonVariant.Tonal -> ClayElevation.Raised
        ClayButtonVariant.OutlinedInset -> ClayElevation.Inset
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.buttonHeight)
            .height(Dimens.buttonHeight)
            .alpha(if (enabled) 1f else 0.5f)
            .clayClickable(
                enabled = enabled,
                shape = shape,
                color = surfaceColor,
                elevation = elevation,
                role = Role.Button,
                isPrimaryAction = variant == ClayButtonVariant.Filled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.spaceLarge),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.invoke()
                    content()
                    trailingIcon?.invoke()
                }
            }
        }
    }
}

@Composable
fun ClayIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: ClayElevation = ClayElevation.Raised,
    color: Color = LocalClayColors.current.face,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(Dimens.minTouchTarget)
            .alpha(if (enabled) 1f else 0.5f)
            .clayClickable(
                enabled = enabled,
                shape = ClayShapes.button,
                color = color,
                elevation = elevation,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
