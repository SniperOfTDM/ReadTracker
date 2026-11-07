package com.sniperoftdm.booktracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.clay
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

object HeatmapLevelCalculator {
    data class Thresholds(val q1: Int, val q2: Int, val q3: Int)

    fun calculateThresholds(pagesByDate: Map<LocalDate, Int>): Thresholds {
        val nonZeroValues = pagesByDate.values.filter { it > 0 }.sorted()
        if (nonZeroValues.isEmpty()) {
            return Thresholds(1, 10, 25)
        }
        val size = nonZeroValues.size
        val q1 = nonZeroValues[(size * 0.25).toInt().coerceIn(0, size - 1)]
        val q2 = nonZeroValues[(size * 0.50).toInt().coerceIn(0, size - 1)]
        val q3 = nonZeroValues[(size * 0.75).toInt().coerceIn(0, size - 1)]
        return Thresholds(
            q1 = q1.coerceAtLeast(1),
            q2 = q2.coerceAtLeast(q1),
            q3 = q3.coerceAtLeast(q2)
        )
    }

    fun intensityLevel(pages: Int, thresholds: Thresholds): Int {
        if (pages <= 0) return 0
        return when {
            pages <= thresholds.q1 -> 1
            pages <= thresholds.q2 -> 2
            pages <= thresholds.q3 -> 3
            else -> 4
        }
    }
}

@Composable
fun ReadingHeatmap(
    pagesPerDay: Map<LocalDate, Int>,
    onDayClick: (LocalDate, Int) -> Unit,
    modifier: Modifier = Modifier,
    weeksCount: Int = 26,
    cellSize: Dp = 15.dp,
    cellSpacing: Dp = 3.dp
) {
    val clayColors = LocalClayColors.current
    val textMeasurer = rememberTextMeasurer()
    val today = remember { LocalDate.now() }

    val thresholds = remember(pagesPerDay) {
        HeatmapLevelCalculator.calculateThresholds(pagesPerDay)
    }

    // Colors for the 5 intensity levels
    val levelColors = remember(clayColors) {
        listOf(
            clayColors.face,                       // Level 0: empty dimple
            clayColors.sage.copy(alpha = 0.40f),  // Level 1: light sage
            clayColors.sage.copy(alpha = 0.70f),  // Level 2: medium sage
            clayColors.sage,                      // Level 3: full sage
            Color(0xFF2E6B5E)                     // Level 4: deep emerald/forest
        )
    }

    // Grid data setup: 26 weeks, columns = weeks, rows = Mon..Sun (0..6)
    // Find current week's Monday
    val currentWeekMonday = remember(today) {
        today.minusDays((today.dayOfWeek.value - 1).toLong())
    }
    val startMonday = remember(currentWeekMonday, weeksCount) {
        currentWeekMonday.minusWeeks((weeksCount - 1).toLong())
    }

    val scrollState = rememberScrollState()

    // Scroll to the latest weeks on initial composition
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val totalPagesInWindow = remember(pagesPerDay, startMonday, today) {
        pagesPerDay.entries
            .filter { (date, _) -> !date.isBefore(startMonday) && !date.isAfter(today) }
            .sumOf { it.value }
    }

    val semanticsDesc = "Reading heatmap grid for the last $weeksCount weeks. Total $totalPagesInWindow pages read."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticsDesc }
    ) {
        // Legend & Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spaceSmall, vertical = Dimens.spaceExtraSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last $weeksCount weeks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Legend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                levelColors.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clay(
                                shape = ClayShapes.small,
                                color = color,
                                elevation = if (index == 0) ClayElevation.Inset else ClayElevation.Raised
                            )
                    )
                }
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Heatmap Content Row: Fixed Weekday Labels on Left + Scrollable Canvas on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clay(
                    shape = ClayShapes.card,
                    color = clayColors.face,
                    elevation = ClayElevation.Inset
                )
                .padding(Dimens.spaceMedium),
            verticalAlignment = Alignment.Top
        ) {
            // Weekday Labels Column (Mon, Wed, Fri)
            Column(
                modifier = Modifier.padding(top = 18.dp, end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val dayLabels = listOf("M", "", "W", "", "F", "", "")
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = cellSize + cellSpacing),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (label.isNotEmpty()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Scrollable Grid Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                val cellSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { cellSize.toPx() }
                val cellSpacingPx = with(androidx.compose.ui.platform.LocalDensity.current) { cellSpacing.toPx() }
                val monthLabelHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 18.dp.toPx() }

                val gridWidthPx = weeksCount * (cellSizePx + cellSpacingPx)
                val gridHeightPx = monthLabelHeightPx + (7 * (cellSizePx + cellSpacingPx))

                val gridWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { gridWidthPx.toDp() }
                val gridHeightDp = with(androidx.compose.ui.platform.LocalDensity.current) { gridHeightPx.toDp() }

                val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                val shadeColor = clayColors.shade
                val terracottaColor = clayColors.terracotta

                Canvas(
                    modifier = Modifier
                        .size(width = gridWidthDp, height = gridHeightDp)
                        .pointerInput(pagesPerDay, startMonday, weeksCount) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val col = ((tapOffset.x) / (cellSizePx + cellSpacingPx)).toInt()
                                    val row = ((tapOffset.y - monthLabelHeightPx) / (cellSizePx + cellSpacingPx)).toInt()

                                    if (col in 0 until weeksCount && row in 0..6) {
                                        val clickedDate = startMonday.plusWeeks(col.toLong()).plusDays(row.toLong())
                                        if (!clickedDate.isAfter(today)) {
                                            val pages = pagesPerDay[clickedDate] ?: 0
                                            onDayClick(clickedDate, pages)
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    var lastDrawnMonth = -1

                    for (weekIndex in 0 until weeksCount) {
                        val weekMonday = startMonday.plusWeeks(weekIndex.toLong())
                        val weekX = weekIndex * (cellSizePx + cellSpacingPx)

                        // Check if we should draw month label
                        val month = weekMonday.monthValue
                        if (month != lastDrawnMonth && weekMonday.dayOfMonth <= 14) {
                            lastDrawnMonth = month
                            val monthStr = weekMonday.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                            val textLayout = textMeasurer.measure(
                                text = monthStr,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = labelColor
                                )
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(weekX, 0f)
                            )
                        }

                        // Draw 7 day pebbles for this week
                        for (dayOfWeekIndex in 0..6) {
                            val date = weekMonday.plusDays(dayOfWeekIndex.toLong())
                            val cellY = monthLabelHeightPx + (dayOfWeekIndex * (cellSizePx + cellSpacingPx))
                            val isFuture = date.isAfter(today)
                            val isToday = date == today
                            val pages = if (isFuture) 0 else (pagesPerDay[date] ?: 0)
                            val level = if (isFuture) 0 else HeatmapLevelCalculator.intensityLevel(pages, thresholds)
                            val pebbleColor = if (isFuture) clayColors.face.copy(alpha = 0.35f) else levelColors[level]

                            val pebbleRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
                            val pebbleRect = RoundRect(
                                left = weekX,
                                top = cellY,
                                right = weekX + cellSizePx,
                                bottom = cellY + cellSizePx,
                                cornerRadius = pebbleRadius
                            )
                            val pebblePath = Path().apply { addRoundRect(pebbleRect) }

                            if (level == 0) {
                                // Inset Dimple
                                drawPath(path = pebblePath, color = pebbleColor)
                                drawPath(
                                    path = pebblePath,
                                    color = shadeColor.copy(alpha = 0.20f),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            } else {
                                // Raised Pebble
                                drawPath(path = pebblePath, color = pebbleColor)

                                // Soft Top Highlight
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.35f),
                                    radius = cellSizePx * 0.28f,
                                    center = Offset(weekX + (cellSizePx * 0.40f), cellY + (cellSizePx * 0.35f))
                                )

                                // Edge
                                drawPath(
                                    path = pebblePath,
                                    color = shadeColor.copy(alpha = 0.15f),
                                    style = Stroke(width = 0.75.dp.toPx())
                                )
                            }

                            // Outline today
                            if (isToday) {
                                drawPath(
                                    path = pebblePath,
                                    color = terracottaColor,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
