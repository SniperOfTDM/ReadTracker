package com.sniperoftdm.booktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.clay
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun BookCoverPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    author: String = "",
    coverPath: String? = null,
    width: Dp = 56.dp,
    height: Dp = 80.dp
) {
    val clayColors = LocalClayColors.current
    val accent = clayColors.accentForTitle(title)
    val shape = RoundedCornerShape(8.dp)

    // Darker spine color
    val spineColor = Color(
        red = (accent.red * 0.70f).coerceIn(0f, 1f),
        green = (accent.green * 0.70f).coerceIn(0f, 1f),
        blue = (accent.blue * 0.70f).coerceIn(0f, 1f),
        alpha = 1f
    )

    val textColor = clayColors.textOnAccent(accent)

    // First letters or initials of title
    val initials = title.trim().split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifEmpty { "B" }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clay(
                shape = shape,
                color = accent,
                elevation = ClayElevation.Raised
            )
            .clip(shape)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Spine strip on the left
            Box(
                modifier = Modifier
                    .width(width * 0.16f)
                    .fillMaxHeight()
                    .background(spineColor)
            )

            // Cover face
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = BookTrackerTypography.bookTitleCover.copy(
                        fontSize = if (width > 60.dp) 18.sp else 13.sp
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
