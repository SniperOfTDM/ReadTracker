package com.sniperoftdm.booktracker.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.ui.clay.ClayBottomSheet
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayCard
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayProgressRing
import com.sniperoftdm.booktracker.ui.clay.ClaySegmentedControl
import com.sniperoftdm.booktracker.ui.components.BookCoverPlaceholder
import com.sniperoftdm.booktracker.ui.components.EmptyState
import com.sniperoftdm.booktracker.ui.components.PagesBarChart
import com.sniperoftdm.booktracker.ui.components.PageStepper
import com.sniperoftdm.booktracker.ui.components.ReadingHeatmap
import com.sniperoftdm.booktracker.ui.components.StatTile
import com.sniperoftdm.booktracker.ui.clay.ClayDialog
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsUiState,
    onPeriodSelected: (StatsPeriod) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onDismissDayDetail: () -> Unit,
    onSetYearlyGoal: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val global = state.globalStats
    var showGoalDialog by remember { mutableStateOf(false) }

    val periods = listOf(
        StatsPeriod.WEEK to stringResource(R.string.period_week),
        StatsPeriod.MONTH to stringResource(R.string.period_month),
        StatsPeriod.ALL_TIME to stringResource(R.string.period_all_time)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Dimens.contentMaxWidth)
        ) {
            // Screen Header
            Text(
                text = stringResource(R.string.nav_stats),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = Dimens.spaceLarge, vertical = Dimens.spaceMedium)
            )

            if (global.totalPagesRead == 0 && global.pagesPerDay.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.spaceLarge),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = stringResource(R.string.empty_stats_title),
                        body = stringResource(R.string.empty_stats_body)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = Dimens.spaceLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceLarge)
                ) {
                    // Period Selector
                    item {
                        ClaySegmentedControl(
                            items = periods,
                            selectedIndex = periods.indexOfFirst { it.first == state.selectedPeriod }.coerceAtLeast(0),
                            onItemSelected = { index -> onPeriodSelected(periods[index].first) }
                        ) { (_, label) ->
                            Text(label, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Reading Bar Chart for selected period
                    item {
                        PagesBarChart(
                            items = state.chartItems,
                            averagePages = state.periodAverage,
                            height = 180.dp
                        )
                    }

                    // Summary StatTiles (2x3 Grid)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                            ) {
                                StatTile(
                                    label = stringResource(R.string.stat_total_pages),
                                    value = "${global.totalPagesRead}",
                                    supporting = "pages read",
                                    accentColor = clayColors.sage,
                                    modifier = Modifier.weight(1f)
                                )
                                val bestDayStr = global.bestDay?.let { (date, p) ->
                                    "$p p (${date.format(DateTimeFormatter.ofPattern("d MMM"))})"
                                } ?: "—"
                                StatTile(
                                    label = stringResource(R.string.stat_best_day),
                                    value = global.bestDay?.second?.toString() ?: "—",
                                    supporting = global.bestDay?.first?.format(DateTimeFormatter.ofPattern("d MMM")),
                                    accentColor = clayColors.peach,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                            ) {
                                StatTile(
                                    label = stringResource(R.string.stat_current_streak),
                                    value = "${global.currentStreak}",
                                    supporting = "days in a row",
                                    accentColor = clayColors.terracotta,
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    label = stringResource(R.string.stat_longest_streak),
                                    value = "${global.longestStreak}",
                                    supporting = "days record",
                                    accentColor = clayColors.butter,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                            ) {
                                StatTile(
                                    label = stringResource(R.string.stat_pages_per_hour),
                                    value = global.pagesPerHour?.let { "${it.toInt()}" } ?: "—",
                                    supporting = "timed speed",
                                    accentColor = clayColors.sky,
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    label = stringResource(R.string.stat_books_finished_year),
                                    value = "${state.booksFinishedThisYear}",
                                    supporting = "this year",
                                    accentColor = clayColors.lilac,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Yearly Goal Card
                    item {
                        val yearlyGoal = state.yearlyGoalProgress
                        if (yearlyGoal != null && yearlyGoal.goal > 0) {
                            ClayCard(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = ClayElevation.Raised,
                                color = clayColors.face,
                                onClick = { showGoalDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ClayProgressRing(
                                            progress = yearlyGoal.progressFraction,
                                            modifier = Modifier.fillMaxSize(),
                                            color = clayColors.terracotta,
                                            strokeWidth = 10.dp
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "${yearlyGoal.finishedCount}/${yearlyGoal.goal}",
                                                    style = BookTrackerTypography.tabularStatValue,
                                                    fontSize = 18.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "books",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(Dimens.spaceLarge))

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = stringResource(R.string.yearly_goal_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        val scheduleDiff = yearlyGoal.aheadBehindSchedule
                                        val scheduleText = when {
                                            scheduleDiff > 0 -> stringResource(
                                                R.string.ahead_of_schedule_format,
                                                String.format("%.1f", scheduleDiff)
                                            )
                                            scheduleDiff < 0 -> stringResource(
                                                R.string.behind_schedule_format,
                                                String.format("%.1f", abs(scheduleDiff))
                                            )
                                            else -> "Right on schedule"
                                        }

                                        Text(
                                            text = scheduleText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (scheduleDiff >= 0) clayColors.sage else MaterialTheme.colorScheme.error
                                        )

                                        Text(
                                            text = stringResource(
                                                R.string.projected_format,
                                                state.projectedBooksByYearEnd
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            ClayCard(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = ClayElevation.Raised,
                                color = clayColors.face,
                                onClick = { showGoalDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.yearly_goal_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.set_yearly_goal_prompt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    ClayButton(
                                        onClick = { showGoalDialog = true },
                                        variant = ClayButtonVariant.Tonal,
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Text("Set Goal")
                                    }
                                }
                            }
                        }
                    }

                    // Shared Total ETA Card
                    state.totalEta?.let { eta ->
                        item {
                            ClayCard(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = ClayElevation.Raised,
                                color = clayColors.sky
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = stringResource(R.string.shared_pace_eta_title),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = clayColors.textOnAccent(clayColors.sky).copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.total_eta_format,
                                            eta.finishDate.format(DateTimeFormatter.ofPattern("MMM d"))
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = clayColors.textOnAccent(clayColors.sky)
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.shared_pace_format,
                                            eta.sharedPace.toInt()
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = clayColors.textOnAccent(clayColors.sky).copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Heatmap (Last 26 Weeks)
                    item {
                        Text(
                            text = stringResource(R.string.reading_activity_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ReadingHeatmap(
                            pagesPerDay = global.pagesPerDay,
                            onDayClick = { date, _ -> onDayClick(date) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimens.spaceHuge))
                    }
                }
            }
        }

        // Day Sessions Drilldown Bottom Sheet
        state.selectedDay?.let { day ->
            ClayBottomSheet(onDismissRequest = onDismissDayDetail) {
                val formattedDate = day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
                Text(
                    text = stringResource(R.string.day_sessions_title, formattedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                if (state.selectedDaySessions.isEmpty()) {
                    Text(
                        text = "No reading logged for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                    ) {
                        state.selectedDaySessions.forEach { dayBook ->
                            ClayCard(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = ClayElevation.Raised,
                                color = clayColors.face
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BookCoverPlaceholder(
                                        title = dayBook.book.title,
                                        author = dayBook.book.author,
                                        width = 44.dp,
                                        height = 60.dp
                                    )

                                    Spacer(modifier = Modifier.width(Dimens.spaceMedium))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dayBook.book.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "+${dayBook.totalPages} pages on this day",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = clayColors.terracotta,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spaceLarge))

                ClayButton(
                    onClick = onDismissDayDetail,
                    variant = ClayButtonVariant.Filled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }

        // Set Yearly Goal Dialog
        if (showGoalDialog) {
            var goalInput by remember {
                mutableStateOf((state.yearlyGoalProgress?.goal ?: 12).coerceAtLeast(1))
            }

            ClayDialog(onDismissRequest = { showGoalDialog = false }) {
                Text(
                    text = stringResource(R.string.yearly_goal_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                Text(
                    text = "How many books do you plan to finish this year? Set 0 to disable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                PageStepper(
                    page = goalInput,
                    totalPages = 365,
                    onPageChange = { goalInput = it.coerceAtLeast(0) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Dimens.spaceLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ClayButton(
                        onClick = { showGoalDialog = false },
                        variant = ClayButtonVariant.OutlinedInset,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Spacer(modifier = Modifier.width(Dimens.spaceSmall))

                    ClayButton(
                        onClick = {
                            onSetYearlyGoal(goalInput)
                            showGoalDialog = false
                        },
                        variant = ClayButtonVariant.Filled,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}
