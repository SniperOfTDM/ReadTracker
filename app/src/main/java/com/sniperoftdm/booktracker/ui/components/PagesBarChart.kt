package com.sniperoftdm.booktracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperoftdm.booktracker.ui.clay.ClayCard
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.clay
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max

data class BarChartItem(
    val date: LocalDate,
    val pages: Int,
    val label: String
)

object ChartAxisHelper {
    fun roundUpMax(maxValue: Int): Int {
        if (maxValue <= 0) return 10
        if (maxValue <= 10) return 10
        if (maxValue <= 25) return 25
        if (maxValue <= 50) return 50
        if (maxValue <= 100) return 100
        val magnitude = Math.pow(10.0, Math.floor(Math.log10(maxValue.toDouble()))).toInt()
        val factor = ceil(maxValue.toDouble() / magnitude.toDouble()).toInt()
        return factor * magnitude
    }
}

@Composable
fun PagesBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    averagePages: Double = 0.0,
    height: Dp = 190.dp
) {
    val clayColors = LocalClayColors.current
    val textMeasurer = rememberTextMeasurer()
    val riseProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        riseProgress.snapTo(0f)
        riseProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val today = remember { LocalDate.now() }
    val maxPages = remember(items) { items.maxOfOrNull { it.pages } ?: 0 }
    val yAxisMax = remember(maxPages, averagePages) {
        ChartAxisHelper.roundUpMax(max(maxPages, ceil(averagePages).toInt()))
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val selectedItem = selectedIndex?.let { if (it in items.indices) items[it] else null }

    val totalPagesRead = remember(items) { items.sumOf { it.pages } }
    val semanticsDesc = "Reading bar chart. Total $totalPagesRead pages across ${items.size} days."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticsDesc }
    ) {
        // Tooltip or Active Selection Bar Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = Dimens.spaceSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedItem != null) {
                Text(
                    text = "${selectedItem.date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}: ${selectedItem.pages} pages",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = if (averagePages > 0) "Avg: ${String.format("%.1f", averagePages)} p/day" else "Tap a bar for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Max: $yAxisMax",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Inset Groove Container with Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clay(
                    shape = ClayShapes.card,
                    color = clayColors.face,
                    elevation = ClayElevation.Inset
                )
                .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceSmall)
        ) {
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            val primaryColor = clayColors.terracotta
            val barColor = clayColors.sage
            val shadeColor = clayColors.shade
            val edgeColor = clayColors.edge

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(items) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (items.isEmpty()) return@detectTapGestures
                                val bottomLabelHeight = 22.dp.toPx()
                                val chartHeight = size.height - bottomLabelHeight
                                if (offset.y > chartHeight) {
                                    selectedIndex = null
                                    return@detectTapGestures
                                }

                                val step = size.width / items.size
                                val index = (offset.x / step).toInt().coerceIn(0, items.lastIndex)
                                selectedIndex = if (selectedIndex == index) null else index
                            }
                        )
                    }
            ) {
                if (items.isEmpty()) return@Canvas

                val bottomLabelHeight = 22.dp.toPx()
                val chartHeight = size.height - bottomLabelHeight
                val chartWidth = size.width
                val barCount = items.size
                val stepX = chartWidth / barCount
                val barWidth = (stepX * 0.58f).coerceIn(4.dp.toPx(), 28.dp.toPx())

                // 1. Draw horizontal gridlines (quarter, half, average)
                val avgY = if (averagePages > 0 && yAxisMax > 0) {
                    chartHeight - ((averagePages / yAxisMax).toFloat() * chartHeight).coerceIn(0f, chartHeight)
                } else null

                if (avgY != null) {
                    val dashPath = Path().apply {
                        moveTo(0f, avgY)
                        lineTo(chartWidth, avgY)
                    }
                    drawPath(
                        path = dashPath,
                        color = primaryColor.copy(alpha = 0.55f),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    )
                }

                // Baseline
                drawLine(
                    color = edgeColor.copy(alpha = 0.35f),
                    start = Offset(0f, chartHeight),
                    end = Offset(chartWidth, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // 2. Draw bars
                items.forEachIndexed { index, item ->
                    val centerX = stepX * index + (stepX / 2f)
                    val barLeft = centerX - (barWidth / 2f)
                    val barFraction = if (yAxisMax > 0) (item.pages.toFloat() / yAxisMax.toFloat()) else 0f
                    val animatedFraction = (barFraction * riseProgress.value).coerceIn(0f, 1f)

                    val isToday = item.date == today
                    val isSelected = selectedIndex == index
                    val activeBarColor = if (isToday || isSelected) primaryColor else barColor

                    val minHeight = if (item.pages > 0) 6.dp.toPx() else 0f
                    val calculatedHeight = chartHeight * animatedFraction
                    val actualHeight = max(minHeight, calculatedHeight)
                    val barTop = chartHeight - actualHeight

                    if (actualHeight > 0f) {
                        val pillRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        val roundRect = RoundRect(
                            left = barLeft,
                            top = barTop,
                            right = barLeft + barWidth,
                            bottom = chartHeight,
                            topLeftCornerRadius = pillRadius,
                            topRightCornerRadius = pillRadius,
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero
                        )
                        val barPath = Path().apply { addRoundRect(roundRect) }

                        // Draw Bar Shadow & Fill
                        drawPath(path = barPath, color = activeBarColor)

                        // Top soft highlight
                        clipPath(barPath) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.45f),
                                radius = barWidth * 0.45f,
                                center = Offset(centerX, barTop + (barWidth * 0.35f))
                            )
                        }

                        // Outline
                        drawPath(
                            path = barPath,
                            color = shadeColor.copy(alpha = 0.18f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // 3. X-axis Labels (Weekday or Day of month)
                    val shouldShowLabel = when {
                        barCount <= 7 -> true
                        barCount <= 14 -> index % 2 == 0
                        else -> index % 5 == 0 || index == barCount - 1
                    }

                    if (shouldShowLabel) {
                        val textLayout = textMeasurer.measure(
                            text = item.label,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) primaryColor else labelColor
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                x = centerX - (textLayout.size.width / 2f),
                                y = chartHeight + 4.dp.toPx()
                            )
                        )
                    }
                }
            }
        }
    }
}
