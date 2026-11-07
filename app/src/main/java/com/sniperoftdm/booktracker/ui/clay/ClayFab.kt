package com.sniperoftdm.booktracker.ui.clay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun ClayFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    expanded: Boolean = false,
    icon: @Composable () -> Unit,
    text: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            .height(56.dp)
            .clayClickable(
                shape = ClayShapes.button,
                color = color,
                elevation = ClayElevation.Floating,
                role = Role.Button,
                isPrimaryAction = true,
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
                    icon()
                    if (text != null) {
                        AnimatedVisibility(visible = expanded) {
                            text()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClayBadge(
    modifier: Modifier = Modifier,
    color: Color = LocalClayColors.current.terracotta,
    contentColor: Color = LocalClayColors.current.textOnAccent(color),
    elevation: ClayElevation = ClayElevation.Raised,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clay(
                shape = ClayShapes.chip,
                color = color,
                elevation = elevation
            )
            .padding(horizontal = Dimens.spaceSmall, vertical = Dimens.spaceExtraSmall),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = MaterialTheme.typography.labelSmall) {
                content()
            }
        }
    }
}
