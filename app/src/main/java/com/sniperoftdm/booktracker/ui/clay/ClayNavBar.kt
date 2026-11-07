package com.sniperoftdm.booktracker.ui.clay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

data class ClayNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any
)

@Composable
fun ClayNavBar(
    items: List<ClayNavItem>,
    currentRoute: Any?,
    onItemClick: (ClayNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.navBarHorizontalMargin,
                vertical = Dimens.navBarBottomMargin
            )
            .height(Dimens.navBarHeight)
            .clay(
                shape = ClayShapes.pill,
                color = clayColors.face,
                elevation = ClayElevation.Floating
            )
            .padding(horizontal = Dimens.spaceMedium),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .semantics {
                            selected = isSelected
                        }
                        .then(
                            if (isSelected) {
                                Modifier.clay(
                                    shape = ClayShapes.pill,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    elevation = ClayElevation.Inset
                                )
                            } else {
                                Modifier.clay(
                                    shape = CircleShape,
                                    color = clayColors.face,
                                    elevation = ClayElevation.Flat
                                )
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onItemClick(item) }
                        )
                        .padding(horizontal = if (isSelected) 16.dp else 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                            AnimatedVisibility(visible = isSelected) {
                                ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
                                    Text(text = item.label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
