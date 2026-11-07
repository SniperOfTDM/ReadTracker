package com.sniperoftdm.booktracker.ui.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.domain.ValidationReason
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayCard
import com.sniperoftdm.booktracker.ui.clay.ClayChip
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.components.BookCoverPlaceholder
import com.sniperoftdm.booktracker.ui.components.DatePickerField
import com.sniperoftdm.booktracker.ui.components.PageStepper
import com.sniperoftdm.booktracker.ui.components.toMessage
import com.sniperoftdm.booktracker.ui.theme.BookTrackerPreviews
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface QuickLogUiAction {
    data class SelectBook(val bookId: Long) : QuickLogUiAction
    data class ChangePage(val page: String) : QuickLogUiAction
    data class StepPage(val delta: Int) : QuickLogUiAction
    data class ChangeBackfill(val timestamp: Long?) : QuickLogUiAction
    data object StartTimer : QuickLogUiAction
    data object CancelTimer : QuickLogUiAction
    data object Submit : QuickLogUiAction
}

@Composable
fun QuickLogContent(
    state: QuickLogUiState,
    onAction: (QuickLogUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val readingBooks = state.books.filter { it.book.status == BookStatus.READING }
    val selectedItem = state.books.find { it.book.id == state.selectedBookId }
        ?: readingBooks.firstOrNull()
        ?: state.books.firstOrNull()

    val selectedBook = selectedItem?.book
    val currentBookPage = selectedItem?.currentPage ?: selectedBook?.startPage ?: 0
    val totalPages = selectedBook?.totalPages ?: 0
    val inputPage = state.pageInput.toIntOrNull()

    // Inline validation logic
    val validationReason: ValidationReason? = when {
        selectedBook == null -> ValidationReason.TITLE_BLANK
        inputPage == null -> ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS
        inputPage <= currentBookPage -> ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS
        inputPage > totalPages -> ValidationReason.END_PAGE_EXCEEDS_TOTAL
        state.backfillTimestamp != null && state.backfillTimestamp > System.currentTimeMillis() -> ValidationReason.TIMESTAMP_IN_FUTURE
        else -> null
    }
    val isValid = validationReason == null

    var isMoreOptionsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spaceMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
    ) {
        // 1. Header & Book Picker
        Text(
            text = stringResource(R.string.quick_log_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (readingBooks.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
            ) {
                readingBooks.forEach { item ->
                    val isSelected = item.book.id == selectedBook?.id
                    ClayChip(
                        selected = isSelected,
                        onClick = { onAction(QuickLogUiAction.SelectBook(item.book.id)) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (item.book.coverPath != null) {
                                BookCoverPlaceholder(
                                    title = item.book.title,
                                    author = item.book.author,
                                    coverPath = item.book.coverPath,
                                    width = 16.dp,
                                    height = 22.dp
                                )
                            }
                            Text(
                                text = "${item.book.title} (p. ${item.currentPage})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else if (selectedBook != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
            ) {
                if (selectedBook.coverPath != null) {
                    BookCoverPlaceholder(
                        title = selectedBook.title,
                        author = selectedBook.author,
                        coverPath = selectedBook.coverPath,
                        width = 24.dp,
                        height = 32.dp
                    )
                }
                Text(
                    text = "${selectedBook.title} · ${selectedBook.author}",
                    style = BookTrackerTypography.bookTitleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Page Stepper
        if (selectedBook != null) {
            val displayPage = inputPage ?: currentBookPage
            PageStepper(
                page = displayPage,
                totalPages = totalPages,
                onPageChange = { newPage ->
                    onAction(QuickLogUiAction.ChangePage(newPage.toString()))
                }
            )

            // 3. Live Preview Line
            val pageDelta = (displayPage - currentBookPage).coerceAtLeast(0)
            val isFinishing = displayPage >= totalPages && totalPages > 0

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (validationReason != null) {
                    Text(
                        text = validationReason.toMessage(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = pluralStringResource(
                            R.plurals.quick_log_preview_pages,
                            pageDelta,
                            pageDelta,
                            currentBookPage,
                            displayPage,
                            totalPages
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (isFinishing) {
                        Text(
                            text = stringResource(R.string.finishes_book_notice),
                            style = MaterialTheme.typography.labelSmall,
                            color = clayColors.terracotta,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Timer Section
        val activeTimer = state.activeTimer
        if (selectedBook != null && activeTimer != null && activeTimer.bookId == selectedBook.id) {
            var elapsedSeconds by remember {
                mutableLongStateOf(((System.currentTimeMillis() - activeTimer.startedAtMillis) / 1000).coerceAtLeast(0))
            }

            LaunchedEffect(activeTimer.startedAtMillis) {
                while (true) {
                    elapsedSeconds = ((System.currentTimeMillis() - activeTimer.startedAtMillis) / 1000).coerceAtLeast(0)
                    delay(1000)
                }
            }

            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            val pagesRead = ((inputPage ?: currentBookPage) - currentBookPage).coerceAtLeast(0)
            val pph = if (elapsedSeconds > 30 && pagesRead > 0) {
                (pagesRead.toDouble() / (elapsedSeconds.toDouble() / 3600.0)).toInt()
            } else null

            ClayCard(
                color = clayColors.butter,
                elevation = ClayElevation.Raised,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.timer_active_format, minutes, seconds),
                            style = BookTrackerTypography.tabularStatValue,
                            color = clayColors.textOnAccent(clayColors.butter)
                        )
                        if (pph != null) {
                            Text(
                                text = pluralStringResource(R.plurals.pages_per_hour, pph, pph),
                                style = MaterialTheme.typography.bodySmall,
                                color = clayColors.textOnAccent(clayColors.butter)
                            )
                        }
                    }

                    ClayButton(
                        onClick = { onAction(QuickLogUiAction.CancelTimer) },
                        variant = ClayButtonVariant.OutlinedInset,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(stringResource(R.string.cancel_timer))
                    }
                }
            }
        } else if (selectedBook != null) {
            ClayButton(
                onClick = { onAction(QuickLogUiAction.StartTimer) },
                variant = ClayButtonVariant.Tonal,
                leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.start_session_timer))
            }
        }

        // 5. Expandable More Options (Backfill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { isMoreOptionsExpanded = !isMoreOptionsExpanded }
                )
                .padding(vertical = Dimens.spaceSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.more_options),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (isMoreOptionsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = isMoreOptionsExpanded) {
            val selectedLocalDate = state.backfillTimestamp?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: LocalDate.now()

            DatePickerField(
                selectedDate = selectedLocalDate,
                onDateSelected = { date ->
                    val timestamp = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    onAction(QuickLogUiAction.ChangeBackfill(timestamp))
                },
                label = stringResource(R.string.checkpoint_date_label),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spaceSmall))

        // 6. Primary Save Button
        ClayButton(
            onClick = { onAction(QuickLogUiAction.Submit) },
            enabled = isValid && !state.isSaving,
            variant = ClayButtonVariant.Filled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

@BookTrackerPreviews
@Composable
private fun QuickLogPreview() {
    com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme {
        QuickLogContent(
            state = QuickLogUiState(
                books = listOf(
                    QuickLogBookItem(
                        book = Book(id = 1, title = "Atomic Habits", author = "James Clear", totalPages = 320, startPage = 0),
                        currentPage = 142
                    )
                ),
                selectedBookId = 1,
                pageInput = "165"
            ),
            onAction = {}
        )
    }
}
