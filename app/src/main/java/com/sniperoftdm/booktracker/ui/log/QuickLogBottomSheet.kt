package com.sniperoftdm.booktracker.ui.log

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.ui.clay.ClayBottomSheet

@Composable
fun QuickLogBottomSheet(
    viewModel: QuickLogViewModel,
    onDismiss: () -> Unit,
    onSaved: (pagesAdded: Int, sessionId: Long, bookId: Long, isFinished: Boolean, book: Book?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuickLogEvent.Saved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDismiss()
                    onSaved(
                        event.pagesAdded,
                        event.sessionId,
                        event.book?.id ?: 0L,
                        event.isFinished,
                        event.book
                    )
                }
                is QuickLogEvent.Error -> {
                    // Handled in UI inline
                }
            }
        }
    }

    ClayBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        QuickLogContent(
            state = state,
            onAction = { action ->
                when (action) {
                    is QuickLogUiAction.SelectBook -> viewModel.selectBook(action.bookId)
                    is QuickLogUiAction.ChangePage -> viewModel.onPageChanged(action.page)
                    is QuickLogUiAction.StepPage -> viewModel.stepPage(action.delta)
                    is QuickLogUiAction.ChangeBackfill -> viewModel.onBackfillTimestampChanged(action.timestamp)
                    is QuickLogUiAction.StartTimer -> viewModel.startTimer()
                    is QuickLogUiAction.CancelTimer -> viewModel.cancelTimer()
                    is QuickLogUiAction.Submit -> viewModel.submit()
                }
            }
        )
    }
}
