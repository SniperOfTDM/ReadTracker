package com.sniperoftdm.booktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.domain.BookStats
import com.sniperoftdm.booktracker.ui.clay.ClayCard
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayIconButton
import com.sniperoftdm.booktracker.ui.clay.ClayProgressBar
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookProgressCard(
    book: Book,
    stats: BookStats?,
    onCardClick: () -> Unit,
    onQuickLogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val currentPage = stats?.currentPage ?: book.startPage
    val progress = stats?.progressPercent ?: if (book.totalPages > 0) {
        currentPage.toFloat() / book.totalPages.toFloat()
    } else 0f
    val percent = (progress * 100).toInt()

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        elevation = ClayElevation.Raised,
        onClick = onCardClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            BookCoverPlaceholder(
                title = book.title,
                author = book.author,
                width = 68.dp,
                height = 96.dp
            )

            Spacer(modifier = Modifier.width(Dimens.spaceMedium))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceExtraSmall)
            ) {
                Text(
                    text = book.title,
                    style = BookTrackerTypography.bookTitleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.author.isNotBlank()) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                ClayProgressBar(progress = progress)

                Text(
                    text = stringResource(R.string.page_progress_format, currentPage, book.totalPages, percent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(Dimens.spaceSmall))

            // Quick Log button on trailing edge
            ClayIconButton(
                onClick = onQuickLogClick,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentDescription = stringResource(R.string.quick_log_title)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
        }

        // Stats Chips (Pace, ETA, Goal)
        if (stats != null) {
            Spacer(modifier = Modifier.height(Dimens.spaceMedium))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
            ) {
                // Pace Chip
                val pace = stats.dailyAverageRecent
                if (pace > 0.0) {
                    StatusChip(
                        status = BookStatus.READING,
                        modifier = Modifier
                    )
                }

                // ETA Chip
                val etaDays = stats.estimatedDaysLeft
                if (etaDays != null && etaDays > 0) {
                    EtaChip(etaText = "~$etaDays days")
                }

                // Goal Status
                val requiredPace = stats.requiredPagesPerDay
                if (requiredPace != null && requiredPace > 0) {
                    val isOnTrack = stats.isOnTrack ?: true
                    GoalChip(
                        isOnTrack = isOnTrack,
                        requiredPagesText = "Need $requiredPace p/day"
                    )
                }
            }
        }
    }
}

@Composable
fun BookRow(
    book: Book,
    stats: BookStats?,
    onRowClick: () -> Unit,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    onMarkFinished: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val currentPage = stats?.currentPage ?: book.startPage
    val progress = stats?.progressPercent ?: if (book.totalPages > 0) {
        currentPage.toFloat() / book.totalPages.toFloat()
    } else 0f

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        elevation = ClayElevation.Flat,
        onClick = onRowClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimens.spaceMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCoverPlaceholder(
                title = book.title,
                author = book.author,
                width = 48.dp,
                height = 68.dp
            )

            Spacer(modifier = Modifier.width(Dimens.spaceMedium))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceExtraSmall)
            ) {
                Text(
                    text = book.title,
                    style = BookTrackerTypography.bookTitleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.author.isNotBlank()) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (book.status == BookStatus.FINISHED) {
                    Text(
                        text = stringResource(R.string.status_finished),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    ClayProgressBar(progress = progress, trackHeight = 8.dp)
                    Text(
                        text = stringResource(R.string.page_counter_format, currentPage, book.totalPages),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimens.spaceSmall))

            StatusChip(status = book.status)

            Box {
                ClayIconButton(
                    onClick = { menuExpanded = true },
                    elevation = ClayElevation.Flat,
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
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (book.status == BookStatus.PAUSED) stringResource(R.string.action_resume)
                                else stringResource(R.string.action_pause)
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
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
