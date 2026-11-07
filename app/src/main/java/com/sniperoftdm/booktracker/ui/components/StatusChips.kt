package com.sniperoftdm.booktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.clay
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun StatusChip(
    status: BookStatus,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val (color, text) = when (status) {
        BookStatus.READING -> clayColors.sage to "Reading"
        BookStatus.PAUSED -> clayColors.butter to "Paused"
        BookStatus.FINISHED -> clayColors.terracotta to "Finished"
    }

    Box(
        modifier = modifier
            .clay(
                shape = ClayShapes.chip,
                color = color,
                elevation = ClayElevation.Raised
            )
            .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceExtraSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = clayColors.textOnAccent(color)
        )
    }
}

@Composable
fun EtaChip(
    etaText: String,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val color = clayColors.sky

    Box(
        modifier = modifier
            .clay(
                shape = ClayShapes.chip,
                color = color,
                elevation = ClayElevation.Raised
            )
            .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceExtraSmall),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = clayColors.textOnAccent(color)
            )
            Text(
                text = etaText,
                style = MaterialTheme.typography.labelSmall,
                color = clayColors.textOnAccent(color)
            )
        }
    }
}

@Composable
fun GoalChip(
    isOnTrack: Boolean,
    requiredPagesText: String,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val color = if (isOnTrack) clayColors.sage else clayColors.peach
    val icon = if (isOnTrack) Icons.Rounded.CheckCircle else Icons.Rounded.Warning

    Box(
        modifier = modifier
            .clay(
                shape = ClayShapes.chip,
                color = color,
                elevation = ClayElevation.Raised
            )
            .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceExtraSmall),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = clayColors.textOnAccent(color)
            )
            Text(
                text = requiredPagesText,
                style = MaterialTheme.typography.labelSmall,
                color = clayColors.textOnAccent(color)
            )
        }
    }
}
