package com.sniperoftdm.booktracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    illustration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.spaceExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (illustration != null) {
            illustration()
        } else {
            ClayBookIllustration(modifier = Modifier.size(140.dp))
        }

        Spacer(modifier = Modifier.height(Dimens.spaceLarge))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Dimens.spaceSmall))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(Dimens.spaceLarge))
            action()
        }
    }
}

@Composable
fun ClayBookIllustration(modifier: Modifier = Modifier) {
    val clayColors = LocalClayColors.current

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Bottom clay book (Lilac)
        drawRoundRect(
            color = clayColors.lilac,
            topLeft = Offset(width * 0.15f, height * 0.55f),
            size = Size(width * 0.70f, height * 0.22f),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )
        // Bottom book spine
        drawRoundRect(
            color = clayColors.lilac.copy(red = clayColors.lilac.red * 0.8f),
            topLeft = Offset(width * 0.15f, height * 0.55f),
            size = Size(width * 0.12f, height * 0.22f),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )

        // Middle clay book (Sage)
        drawRoundRect(
            color = clayColors.sage,
            topLeft = Offset(width * 0.22f, height * 0.38f),
            size = Size(width * 0.65f, height * 0.20f),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
        )
        // Middle book spine
        drawRoundRect(
            color = clayColors.sage.copy(red = clayColors.sage.red * 0.8f),
            topLeft = Offset(width * 0.22f, height * 0.38f),
            size = Size(width * 0.11f, height * 0.20f),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
        )

        // Top clay book with bookmark (Peach + Terracotta bookmark)
        drawRoundRect(
            color = clayColors.peach,
            topLeft = Offset(width * 0.28f, height * 0.22f),
            size = Size(width * 0.58f, height * 0.18f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        // Bookmark ribbon hanging down
        drawRoundRect(
            color = clayColors.terracotta,
            topLeft = Offset(width * 0.62f, height * 0.28f),
            size = Size(width * 0.08f, height * 0.25f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Top book spine
        drawRoundRect(
            color = clayColors.peach.copy(red = clayColors.peach.red * 0.8f),
            topLeft = Offset(width * 0.28f, height * 0.22f),
            size = Size(width * 0.10f, height * 0.18f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
    }
}
