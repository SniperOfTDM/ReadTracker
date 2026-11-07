package com.sniperoftdm.booktracker.ui.clay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LightInk
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun ClayChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ClayShapes.chip,
    activeColor: Color = LocalClayColors.current.sage,
    inactiveColor: Color = LocalClayColors.current.face,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit
) {
    val clayColors = LocalClayColors.current
    val surfaceColor = if (selected) activeColor else inactiveColor
    val elevation = if (selected) ClayElevation.Inset else ClayElevation.Raised

    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val contentColor = if (selected) {
        if (activeColor == primaryContainer) {
            onPrimaryContainer
        } else {
            clayColors.textOnAccent(activeColor)
        }
    } else {
        clayColors.contentColorFor(inactiveColor)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 38.dp)
            .clayClickable(
                shape = shape,
                color = surfaceColor,
                elevation = elevation,
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceSmall),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.invoke()
                    label()
                    trailingIcon?.invoke()
                }
            }
        }
    }
}
