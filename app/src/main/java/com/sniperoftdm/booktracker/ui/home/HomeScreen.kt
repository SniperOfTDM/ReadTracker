package com.sniperoftdm.booktracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperoftdm.booktracker.R
import androidx.compose.ui.res.pluralStringResource
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayScreenScaffold
import com.sniperoftdm.booktracker.ui.clay.ClayCard
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayFab
import com.sniperoftdm.booktracker.ui.clay.ClayProgressRing
import com.sniperoftdm.booktracker.ui.clay.clay
import com.sniperoftdm.booktracker.ui.components.BookProgressCard
import com.sniperoftdm.booktracker.ui.components.EmptyState
import com.sniperoftdm.booktracker.ui.components.SectionHeader
import com.sniperoftdm.booktracker.ui.theme.BookTrackerPreviews
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddBook: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToStats: () -> Unit,
    onOpenQuickLog: (Long?) -> Unit,
    onStopTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val listState = rememberLazyListState()
    val formattedDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    }

    ClayScreenScaffold(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        subtitle = {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        floatingActionButton = {
            ClayFab(
                onClick = { onOpenQuickLog(null) },
                expanded = !listState.isScrollInProgress,
                icon = { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.quick_log_title)) },
                text = { Text(stringResource(R.string.shortcut_log_pages_short)) }
            )
        },
        isTopLevel = true
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Dimens.contentMaxWidth),
            contentPadding = PaddingValues(
                start = Dimens.spaceLarge,
                end = Dimens.spaceLarge,
                top = Dimens.spaceSmall,
                bottom = paddingValues.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceLarge)
        ) {
            // 1. Today Summary Hero Card
            item(key = "hero_today") {
                TodaySummaryCard(
                    todayPages = state.todayPages,
                    booksTouchedToday = state.booksTouchedToday,
                    currentStreak = state.currentStreak,
                    encouragement = state.encouragementText
                )
            }

            // 2. Active Timer Banner
            if (state.activeTimer != null) {
                item(key = "active_timer") {
                    ActiveTimerBanner(
                        bookTitle = state.activeTimerBookTitle ?: stringResource(R.string.select_book),
                        startedAtMillis = state.activeTimer.startedAtMillis,
                        onStopClick = onStopTimer
                    )
                }
            }

            // 3. Reading Now Section
            if (state.totalBooksCount == 0) {
                item(key = "empty_no_books") {
                    EmptyState(
                        title = stringResource(R.string.empty_library_title),
                        body = stringResource(R.string.empty_library_body),
                        action = {
                            ClayButton(
                                onClick = onNavigateToAddBook,
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) }
                            ) {
                                Text(stringResource(R.string.add_book_title))
                            }
                        }
                    )
                }
            } else if (state.readingBooks.isEmpty()) {
                item(key = "empty_no_reading") {
                    EmptyState(
                        title = stringResource(R.string.no_reading_books_title),
                        body = stringResource(R.string.no_reading_books_body),
                        action = {
                            ClayButton(
                                onClick = onNavigateToLibrary,
                                variant = ClayButtonVariant.Tonal
                            ) {
                                Text(stringResource(R.string.nav_library))
                            }
                        }
                    )
                }
            } else {
                item(key = "reading_now_header") {
                    SectionHeader(title = stringResource(R.string.reading_now_header))
                }

                items(
                    items = state.readingBooks,
                    key = { it.book.id }
                ) { bookItem ->
                    BookProgressCard(
                        book = bookItem.book,
                        stats = bookItem.stats,
                        onCardClick = { onNavigateToDetail(bookItem.book.id) },
                        onQuickLogClick = { onOpenQuickLog(bookItem.book.id) }
                    )
                }

                // 4. Total ETA Line & Small Yearly Goal Ring
                if (state.totalEta != null || state.yearlyGoalProgress != null) {
                    item(key = "total_eta_card") {
                        TotalEtaCard(
                            totalEta = state.totalEta,
                            yearlyGoalProgress = state.yearlyGoalProgress,
                            onStatsClick = onNavigateToStats
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(
    todayPages: Int,
    booksTouchedToday: Int,
    currentStreak: Int,
    encouragement: String,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        elevation = ClayElevation.Raised,
        contentPadding = PaddingValues(Dimens.spaceLarge)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.read_today_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = todayPages.toString(),
                    style = BookTrackerTypography.tabularHeroValue,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = encouragement,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Streak Orb
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clay(
                        shape = CircleShape,
                        color = clayColors.terracotta,
                        elevation = ClayElevation.Raised
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = stringResource(R.string.streak_label),
                        tint = clayColors.textOnAccent(clayColors.terracotta),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "$currentStreak d",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = clayColors.textOnAccent(clayColors.terracotta)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTimerBanner(
    bookTitle: String,
    startedAtMillis: Long,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    var elapsedSeconds by remember {
        mutableLongStateOf(((System.currentTimeMillis() - startedAtMillis) / 1000).coerceAtLeast(0))
    }

    LaunchedEffect(startedAtMillis) {
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - startedAtMillis) / 1000).coerceAtLeast(0)
            delay(1000)
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        color = clayColors.butter,
        elevation = ClayElevation.Raised
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.timer_active_for, bookTitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = clayColors.textOnAccent(clayColors.butter)
                )
                Text(
                    text = stringResource(R.string.timer_active_format, minutes, seconds),
                    style = BookTrackerTypography.tabularStatValue,
                    color = clayColors.textOnAccent(clayColors.butter)
                )
            }

            ClayButton(
                onClick = onStopClick,
                variant = ClayButtonVariant.Filled,
                leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null) },
                modifier = Modifier.height(44.dp)
            ) {
                Text(stringResource(R.string.action_stop_and_log))
            }
        }
    }
}

@Composable
private fun TotalEtaCard(
    totalEta: com.sniperoftdm.booktracker.domain.TotalEta?,
    yearlyGoalProgress: com.sniperoftdm.booktracker.domain.YearlyGoalProgress?,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current

    ClayCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onStatsClick
            ),
        elevation = ClayElevation.Flat
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (totalEta != null) {
                    val finishDate = totalEta.finishDate
                    val formattedFinish = finishDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
                    Text(
                        text = stringResource(R.string.total_eta_format, formattedFinish),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val sharedPaceInt = totalEta.sharedPace.toInt()
                    Text(
                        text = pluralStringResource(R.plurals.shared_pace, sharedPaceInt, sharedPaceInt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (yearlyGoalProgress != null) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.books_finished_this_year,
                            yearlyGoalProgress.goal,
                            yearlyGoalProgress.finishedCount,
                            yearlyGoalProgress.goal
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (yearlyGoalProgress != null) {
                val progress = (yearlyGoalProgress.finishedCount.toFloat() / yearlyGoalProgress.goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .padding(start = Dimens.spaceSmall),
                    contentAlignment = Alignment.Center
                ) {
                    ClayProgressRing(
                        progress = progress,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 6.dp
                    ) {
                        Text(
                            text = "${yearlyGoalProgress.finishedCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@BookTrackerPreviews
@Composable
private fun HomePreview() {
    com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme {
        HomeScreen(
            state = HomeUiState(
                todayPages = 42,
                booksTouchedToday = 2,
                currentStreak = 7,
                encouragementText = "42 pages today",
                totalBooksCount = 3
            ),
            onNavigateToDetail = {},
            onNavigateToAddBook = {},
            onNavigateToLibrary = {},
            onNavigateToStats = {},
            onOpenQuickLog = {},
            onStopTimer = {}
        )
    }
}
