package com.sniperoftdm.booktracker.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayChip
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayFab
import com.sniperoftdm.booktracker.ui.clay.ClayIconButton
import com.sniperoftdm.booktracker.ui.components.BookRow
import com.sniperoftdm.booktracker.ui.components.ConfirmDialog
import com.sniperoftdm.booktracker.ui.components.EmptyState
import com.sniperoftdm.booktracker.ui.theme.BookTrackerPreviews
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onFilterChange: (BookStatus?) -> Unit,
    onSortChange: (LibrarySortOrder) -> Unit,
    onBookClick: (Long) -> Unit,
    onNavigateToAddBook: () -> Unit,
    onNavigateToEditBook: (Long) -> Unit,
    onTogglePause: (Book) -> Unit,
    onMarkFinished: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val listState = rememberLazyListState()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Dimens.contentMaxWidth),
            contentPadding = PaddingValues(
                start = Dimens.spaceLarge,
                end = Dimens.spaceLarge,
                top = Dimens.spaceLarge,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
        ) {
            // Header with Title and Sort Menu
            item(key = "library_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.nav_library),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box {
                        ClayIconButton(
                            onClick = { sortMenuExpanded = true },
                            elevation = ClayElevation.Raised,
                            contentDescription = stringResource(R.string.sort_options)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_last_read)) },
                                onClick = {
                                    onSortChange(LibrarySortOrder.LAST_READ)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_title)) },
                                onClick = {
                                    onSortChange(LibrarySortOrder.TITLE)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_progress)) },
                                onClick = {
                                    onSortChange(LibrarySortOrder.PROGRESS)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_date_added)) },
                                onClick = {
                                    onSortChange(LibrarySortOrder.DATE_ADDED)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Filters Row (Reading, Paused, Finished, All)
            item(key = "filters_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
                ) {
                    ClayChip(
                        selected = state.selectedFilter == BookStatus.READING,
                        onClick = { onFilterChange(BookStatus.READING) },
                        activeColor = clayColors.sage
                    ) {
                        Text("${stringResource(R.string.filter_reading)} (${state.countReading})")
                    }

                    ClayChip(
                        selected = state.selectedFilter == BookStatus.PAUSED,
                        onClick = { onFilterChange(BookStatus.PAUSED) },
                        activeColor = clayColors.butter
                    ) {
                        Text("${stringResource(R.string.filter_paused)} (${state.countPaused})")
                    }

                    ClayChip(
                        selected = state.selectedFilter == BookStatus.FINISHED,
                        onClick = { onFilterChange(BookStatus.FINISHED) },
                        activeColor = clayColors.terracotta
                    ) {
                        Text("${stringResource(R.string.filter_finished)} (${state.countFinished})")
                    }

                    ClayChip(
                        selected = state.selectedFilter == null,
                        onClick = { onFilterChange(null) },
                        activeColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text("${stringResource(R.string.filter_all)} (${state.countAll})")
                    }
                }
            }

            // Books list or Empty State
            if (state.books.isEmpty()) {
                item(key = "library_empty") {
                    val (emptyTitle, emptyBody) = when (state.selectedFilter) {
                        BookStatus.READING -> stringResource(R.string.empty_reading_title) to stringResource(R.string.empty_reading_body)
                        BookStatus.PAUSED -> stringResource(R.string.empty_paused_title) to stringResource(R.string.empty_paused_body)
                        BookStatus.FINISHED -> stringResource(R.string.empty_finished_title) to stringResource(R.string.empty_finished_body)
                        null -> stringResource(R.string.empty_library_title) to stringResource(R.string.empty_library_body)
                    }

                    EmptyState(
                        title = emptyTitle,
                        body = emptyBody,
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
            } else {
                items(
                    items = state.books,
                    key = { it.book.id }
                ) { item ->
                    BookRow(
                        book = item.book,
                        stats = item.stats,
                        onRowClick = { onBookClick(item.book.id) },
                        onEdit = { onNavigateToEditBook(item.book.id) },
                        onTogglePause = { onTogglePause(item.book) },
                        onMarkFinished = { onMarkFinished(item.book) },
                        onDelete = { bookToDelete = item.book }
                    )
                }
            }
        }

        // Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Dimens.spaceLarge, bottom = 96.dp)
        ) {
            ClayFab(
                onClick = onNavigateToAddBook,
                expanded = !listState.isScrollInProgress,
                icon = { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_book_title)) },
                text = { Text(stringResource(R.string.add_book_title)) }
            )
        }
    }

    // Delete Confirmation Dialog
    if (bookToDelete != null) {
        val book = bookToDelete!!
        ConfirmDialog(
            title = stringResource(R.string.delete_book_title),
            message = stringResource(R.string.delete_book_message, book.title),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                onDeleteBook(book)
                bookToDelete = null
            },
            onDismiss = { bookToDelete = null }
        )
    }
}

@BookTrackerPreviews
@Composable
private fun LibraryPreview() {
    com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme {
        LibraryScreen(
            state = LibraryUiState(
                countAll = 4,
                countReading = 2,
                countPaused = 1,
                countFinished = 1,
                books = listOf(
                    BookItemUi(
                        book = Book(id = 1, title = "The Hobbit", author = "J.R.R. Tolkien", totalPages = 310, startPage = 0),
                        stats = com.sniperoftdm.booktracker.domain.BookStats(
                            currentPage = 120,
                            pagesLeft = 190,
                            progressPercent = 0.38f,
                            todayPages = 15,
                            dailyAverageAllTime = 20.0,
                            dailyAverageRecent = 22.0,
                            estimatedDaysLeft = 9,
                            estimatedFinishDate = java.time.LocalDate.now().plusDays(9),
                            requiredPagesPerDay = null,
                            isOnTrack = null,
                            pagesPerDay = emptyMap(),
                            sessionsWithPages = emptyList()
                        )
                    )
                )
            ),
            onFilterChange = {},
            onSortChange = {},
            onBookClick = {},
            onNavigateToAddBook = {},
            onNavigateToEditBook = {},
            onTogglePause = {},
            onMarkFinished = {},
            onDeleteBook = {}
        )
    }
}
