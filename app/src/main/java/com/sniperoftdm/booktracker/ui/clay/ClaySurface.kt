package com.sniperoftdm.booktracker.ui.clay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun ClaySurface(
    modifier: Modifier = Modifier,
    shape: Shape = ClayShapes.card,
    color: Color = LocalClayColors.current.face,
    elevation: ClayElevation = ClayElevation.Raised,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.clay(
            shape = shape,
            color = color,
            elevation = elevation
        ),
        content = content
    )
}

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    shape: Shape = ClayShapes.card,
    color: Color = LocalClayColors.current.face,
    elevation: ClayElevation = ClayElevation.Raised,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(Dimens.spaceLarge),
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val clayModifier = if (onClick != null) {
        modifier.clayClickable(
            shape = shape,
            color = color,
            elevation = elevation,
            onClick = onClick
        )
    } else {
        modifier.clay(
            shape = shape,
            color = color,
            elevation = elevation
        )
    }

    Box(modifier = clayModifier) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
