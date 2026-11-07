package com.sniperoftdm.booktracker.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.sniperoftdm.booktracker.ui.components.BookCoverPlaceholder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Note
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.domain.BookStats
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayCard
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayIconButton
import com.sniperoftdm.booktracker.ui.clay.ClayProgressRing
import com.sniperoftdm.booktracker.ui.clay.ClaySegmentedControl
import com.sniperoftdm.booktracker.ui.clay.ClayTextField
import com.sniperoftdm.booktracker.ui.clay.clay
import com.sniperoftdm.booktracker.ui.components.EmptyState
import com.sniperoftdm.booktracker.ui.components.GoalChip
import com.sniperoftdm.booktracker.ui.components.PagesBarChart
import com.sniperoftdm.booktracker.ui.components.StatTile
import com.sniperoftdm.booktracker.ui.components.StatusChip
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    state: BookDetailUiState,
    onBack: () -> Unit,
    onNavigateToEditBook: (Long) -> Unit,
    onQuickLogClick: (Long) -> Unit,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onTogglePause: () -> Unit,
    onMarkFinished: () -> Unit,
    onDeleteBook: () -> Unit,
    onEditSession: (Session) -> Unit,
    onDeleteSession: (Session) -> Unit,
    onUndoDeleteSession: () -> Unit,
    onAddNote: (Int, String) -> Unit,
    onEditNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onUndoDeleteNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var menuExpanded by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<Session?>(null) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }

    val book = state.book
    val stats = state.stats

    if (book == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Book not found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val progress = stats?.progressPercent ?: 0f
    val percent = (progress * 100).toInt()
    val currentPage = stats?.currentPage ?: book.startPage

    val pagerState = rememberPagerState(pageCount = { 2 })
    val tabs = listOf(
        stringResource(R.string.tab_days),
        stringResource(R.string.tab_notes)
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
                .navigationBarsPadding()
        ) {
            // Collapsing Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClayIconButton(
                    onClick = onBack,
                    elevation = ClayElevation.Raised,
                    contentDescription = stringResource(R.string.action_cancel)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(status = book.status)

                    Box {
                        ClayIconButton(
                            onClick = { menuExpanded = true },
                            elevation = ClayElevation.Raised,
                            contentDescription = stringResource(R.string.more_options)
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToEditBook(book.id)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (book.status == BookStatus.PAUSED) {
                                            stringResource(R.string.action_resume)
                                        } else {
                                            stringResource(R.string.action_pause)
                                        }
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onTogglePause()
                                }
                            )
                            if (book.status != BookStatus.FINISHED) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_mark_finished)) },
                                    onClick = {
                                        menuExpanded = false
                                        onMarkFinished()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.action_delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteBook()
                                }
                            )
                        }
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = Dimens.spaceLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceLarge)
            ) {
                // Book Title & Author Header
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BookCoverPlaceholder(
                            title = book.title,
                            author = book.author,
                            coverPath = book.coverPath,
                            width = 72.dp,
                            height = 104.dp
                        )
                        Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                        Text(
                            text = book.title,
                            style = BookTrackerTypography.bookTitleLarge,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (book.author.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Header Progress Ring Hero
                item {
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = ClayElevation.Raised,
                        color = clayColors.face
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                        ) {
                            Box(
                                modifier = Modifier.size(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ClayProgressRing(
                                    progress = progress,
                                    modifier = Modifier.fillMaxSize(),
                                    color = clayColors.sage,
                                    strokeWidth = 14.dp
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$percent%",
                                            style = BookTrackerTypography.tabularStatValue,
                                            fontSize = 32.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.page_counter_format,
                                                currentPage,
                                                book.totalPages
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Day badge & Goal chip
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clay(
                                            shape = ClayShapes.chip,
                                            color = clayColors.butter,
                                            elevation = ClayElevation.Raised
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.day_badge_format, state.daysReadingCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = clayColors.textOnAccent(clayColors.butter)
                                    )
                                }

                                if (stats?.requiredPagesPerDay != null && stats.isOnTrack != null) {
                                    GoalChip(
                                        isOnTrack = stats.isOnTrack,
                                        requiredPagesText = "Need ${stats.requiredPagesPerDay} p/day"
                                    )
                                }
                            }
                        }
                    }
                }

                // Primary Action Buttons: Log Pages & Start Timer
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                    ) {
                        ClayButton(
                            onClick = { onQuickLogClick(book.id) },
                            modifier = Modifier.weight(1f),
                            variant = ClayButtonVariant.Filled,
                            leadingIcon = {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                            }
                        ) {
                            Text(stringResource(R.string.shortcut_log_pages_short))
                        }

                        if (state.isTimerActive) {
                            ClayButton(
                                onClick = onCancelTimer,
                                modifier = Modifier.weight(1f),
                                variant = ClayButtonVariant.Tonal,
                                leadingIcon = {
                                    Icon(Icons.Rounded.Stop, contentDescription = null)
                                }
                            ) {
                                Text(stringResource(R.string.cancel_timer))
                            }
                        } else {
                            ClayButton(
                                onClick = onStartTimer,
                                modifier = Modifier.weight(1f),
                                variant = ClayButtonVariant.OutlinedInset,
                                leadingIcon = {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                }
                            ) {
                                Text(stringResource(R.string.start_session_timer))
                            }
                        }
                    }
                }

                // Grid of StatTiles
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                        ) {
                            StatTile(
                                label = stringResource(R.string.daily_average_label),
                                value = String.format("%.1f", stats?.dailyAverageRecent ?: 0.0),
                                supporting = "pages/day",
                                accentColor = clayColors.peach,
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = stringResource(R.string.last_7_days_label),
                                value = "${stats?.pagesPerDay?.entries?.filter { !it.key.isBefore(LocalDate.now().minusDays(6)) }?.sumOf { it.value } ?: 0}",
                                supporting = "pages",
                                accentColor = clayColors.sky,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                        ) {
                            StatTile(
                                label = stringResource(R.string.pages_left_label),
                                value = "${stats?.pagesLeft ?: (book.totalPages - currentPage)}",
                                supporting = "remaining",
                                accentColor = clayColors.butter,
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = stringResource(R.string.eta_label),
                                value = stats?.estimatedFinishDate?.format(DateTimeFormatter.ofPattern("MMM d")) ?: "—",
                                supporting = stats?.estimatedDaysLeft?.let { "$it days left" } ?: "no pace yet",
                                accentColor = clayColors.lilac,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (stats?.pagesPerHour != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                            ) {
                                StatTile(
                                    label = stringResource(R.string.pages_per_hour_label),
                                    value = "${stats.pagesPerHour.toInt()}",
                                    supporting = "reading speed",
                                    accentColor = clayColors.sage,
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    label = stringResource(R.string.days_reading_label),
                                    value = "${state.daysReadingCount}",
                                    supporting = "days total",
                                    accentColor = clayColors.terracotta,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 14-Day Activity Bar Chart
                item {
                    Text(
                        text = stringResource(R.string.reading_activity_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PagesBarChart(
                        items = state.last14Days,
                        averagePages = stats?.dailyAverageRecent ?: 0.0,
                        height = 150.dp
                    )
                }

                // Tab Selector: Days vs Notes
                item {
                    ClaySegmentedControl(
                        items = tabs,
                        selectedIndex = pagerState.currentPage,
                        onItemSelected = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    ) { label ->
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Tab Page Content
                if (pagerState.currentPage == 0) {
                    // DAYS TAB
                    if (state.dayListItems.isEmpty()) {
                        item {
                            EmptyState(
                                title = stringResource(R.string.empty_library_title),
                                body = stringResource(R.string.empty_library_body)
                            )
                        }
                    } else {
                        state.dayListItems.forEach { item ->
                            when (item) {
                                is DayListItem.ZeroReadingGap -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clay(
                                                    shape = ClayShapes.small,
                                                    color = clayColors.face,
                                                    elevation = ClayElevation.Inset
                                                )
                                                .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceSmall),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pluralStringResource(R.plurals.no_reading_days, item.daysCount, item.daysCount),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                is DayListItem.ReadingDay -> {
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
                                        ) {
                                            // Sticky day header
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Day ${item.dayNumber} · ${item.date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = "+${item.totalPagesRead} pages",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = clayColors.terracotta,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            // Session rows
                                            item.sessions.forEach { (session, pagesGained) ->
                                                val dismissState = rememberSwipeToDismissBoxState(
                                                    confirmValueChange = { value ->
                                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                                            onDeleteSession(session)
                                                            scope.launch {
                                                                val result = snackbarHostState.showSnackbar(
                                                                    message = "Session deleted",
                                                                    actionLabel = "Undo",
                                                                    duration = SnackbarDuration.Short
                                                                )
                                                                if (result == SnackbarResult.ActionPerformed) {
                                                                    onUndoDeleteSession()
                                                                }
                                                            }
                                                            true
                                                        } else false
                                                    }
                                                )

                                                SwipeToDismissBox(
                                                    state = dismissState,
                                                    enableDismissFromStartToEnd = false,
                                                    backgroundContent = {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(ClayShapes.card)
                                                                .background(MaterialTheme.colorScheme.error)
                                                                .padding(horizontal = Dimens.spaceLarge),
                                                            contentAlignment = Alignment.CenterEnd
                                                        ) {
                                                            Icon(
                                                                Icons.Rounded.Delete,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onError
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    ClayCard(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        elevation = ClayElevation.Raised,
                                                        color = clayColors.face,
                                                        onClick = { sessionToEdit = session }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column {
                                                                val timeStr = Instant.ofEpochMilli(session.timestamp)
                                                                    .atZone(ZoneId.systemDefault())
                                                                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                                                                Text(
                                                                    text = timeStr,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                Text(
                                                                    text = stringResource(R.string.now_on_page_format, session.endPage),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }

                                                            Column(horizontalAlignment = Alignment.End) {
                                                                Text(
                                                                    text = "+$pagesGained p",
                                                                    style = MaterialTheme.typography.titleSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = clayColors.sage
                                                                )
                                                                if (session.durationSeconds != null && session.durationSeconds > 0) {
                                                                    Text(
                                                                        text = "${session.durationSeconds / 60}m",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // NOTES TAB
                    item {
                        var newNotePage by remember { mutableStateOf(currentPage.toString()) }
                        var newNoteText by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                        ) {
                            // Add note card
                            ClayCard(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = ClayElevation.Raised,
                                color = clayColors.face
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ClayTextField(
                                        value = newNotePage,
                                        onValueChange = { newNotePage = it.filter { char -> char.isDigit() } },
                                        label = "Page",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(80.dp)
                                    )

                                    ClayTextField(
                                        value = newNoteText,
                                        onValueChange = { newNoteText = it },
                                        label = stringResource(R.string.add_note_placeholder),
                                        modifier = Modifier.weight(1f)
                                    )

                                    ClayIconButton(
                                        onClick = {
                                            val pageNum = newNotePage.toIntOrNull() ?: currentPage
                                            if (newNoteText.isNotBlank()) {
                                                onAddNote(pageNum, newNoteText.trim())
                                                newNoteText = ""
                                            }
                                        },
                                        elevation = ClayElevation.Raised,
                                        contentDescription = "Save Note"
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                                    }
                                }
                            }

                            if (state.notes.isEmpty()) {
                                EmptyState(
                                    title = stringResource(R.string.empty_notes_title),
                                    body = stringResource(R.string.empty_notes_body)
                                )
                            } else {
                                state.notes.forEachIndexed { index, note ->
                                    val noteAccent = clayColors.accents[index % clayColors.accents.size]

                                    ClayCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = ClayElevation.Raised,
                                        color = noteAccent,
                                        onClick = { noteToEdit = note }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clay(
                                                        shape = ClayShapes.chip,
                                                        color = clayColors.face,
                                                        elevation = ClayElevation.Raised
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Page ${note.page}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = clayColors.textOnAccent(noteAccent)
                                                )
                                            }

                                            ClayIconButton(
                                                onClick = {
                                                    onDeleteNote(note)
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Note deleted",
                                                            actionLabel = "Undo",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            onUndoDeleteNote()
                                                        }
                                                    }
                                                },
                                                elevation = ClayElevation.Flat,
                                                contentDescription = "Delete Note"
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = note.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = clayColors.textOnAccent(noteAccent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Dimens.spaceHuge))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Session Edit Dialog
        sessionToEdit?.let { session ->
            EditSessionDialog(
                session = session,
                totalPages = book.totalPages,
                onConfirm = { updated ->
                    onEditSession(updated)
                    sessionToEdit = null
                },
                onDismiss = { sessionToEdit = null }
            )
        }

        // Note Edit Dialog
        noteToEdit?.let { note ->
            EditNoteDialog(
                note = note,
                totalPages = book.totalPages,
                onConfirm = { updated ->
                    onEditNote(updated)
                    noteToEdit = null
                },
                onDismiss = { noteToEdit = null }
            )
        }
    }
}
