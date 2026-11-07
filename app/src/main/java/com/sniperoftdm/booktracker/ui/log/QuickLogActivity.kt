package com.sniperoftdm.booktracker.ui.log

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sniperoftdm.booktracker.BookTrackerApp
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.clay
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

class QuickLogActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
    }

    private var viewModel: QuickLogViewModel? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newBookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L).takeIf { it > 0 }
        if (newBookId != null) {
            viewModel?.selectBook(newBookId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as BookTrackerApp).container
        val preselectedBookId = intent?.getLongExtra(EXTRA_BOOK_ID, -1L)?.takeIf { it > 0 }

        setContent {
            BookTrackerTheme {
                val vm = remember {
                    QuickLogViewModel(
                        repository = container.repository,
                        timerRepository = container.timerRepository
                    ).apply {
                        if (preselectedBookId != null) {
                            selectBook(preselectedBookId)
                        }
                        this@QuickLogActivity.viewModel = this
                    }
                }

                QuickLogActivityDialog(
                    viewModel = vm,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun QuickLogActivityDialog(
    viewModel: QuickLogViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clayColors = LocalClayColors.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuickLogEvent.Saved -> onDismiss()
                is QuickLogEvent.Error -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Intercept dismissal
                )
                .clay(
                    shape = ClayShapes.dialog,
                    color = clayColors.face,
                    elevation = ClayElevation.Floating
                )
                .imePadding()
                .padding(Dimens.spaceLarge)
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
}
