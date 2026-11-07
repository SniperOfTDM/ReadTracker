package com.sniperoftdm.booktracker.ui.book

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayIconButton
import com.sniperoftdm.booktracker.ui.clay.ClaySegmentedControl
import com.sniperoftdm.booktracker.ui.clay.ClayTextField
import com.sniperoftdm.booktracker.ui.components.BookCoverPlaceholder
import com.sniperoftdm.booktracker.ui.components.ConfirmDialog
import com.sniperoftdm.booktracker.ui.components.DatePickerField
import com.sniperoftdm.booktracker.ui.components.toMessage
import com.sniperoftdm.booktracker.ui.theme.BookTrackerPreviews
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun AddEditBookScreen(
    state: AddEditBookUiState,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onTotalPagesChange: (String) -> Unit,
    onStartPageChange: (String) -> Unit,
    onGoalDateChange: (java.time.LocalDate?) -> Unit,
    onStatusChange: (BookStatus) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val focusManager = LocalFocusManager.current
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Intercept back navigation if changes are pending
    BackHandler(enabled = state.hasUnsavedChanges) {
        showDiscardDialog = true
    }

    val validation = when {
        state.title.isBlank() -> null // Prompt user by disabling save until entered
        state.totalPagesInput.toIntOrNull() == null || state.totalPagesInput.toInt() <= 0 -> com.sniperoftdm.booktracker.domain.ValidationReason.TOTAL_PAGES_NOT_POSITIVE
        state.startPageInput.toIntOrNull() == null || state.startPageInput.toInt() < 0 -> com.sniperoftdm.booktracker.domain.ValidationReason.START_PAGE_OUT_OF_RANGE
        state.totalPagesInput.toIntOrNull() != null && state.startPageInput.toIntOrNull() != null &&
                state.startPageInput.toInt() > state.totalPagesInput.toInt() -> com.sniperoftdm.booktracker.domain.ValidationReason.START_PAGE_OUT_OF_RANGE
        state.isEditing &&
                (state.totalPagesInput.toIntOrNull() ?: 0) < state.currentPage -> com.sniperoftdm.booktracker.domain.ValidationReason.TOTAL_PAGES_BELOW_CURRENT
        else -> null
    }

    val isValid = state.title.isNotBlank() &&
            state.totalPagesInput.toIntOrNull() != null &&
            state.totalPagesInput.toInt() > 0 &&
            validation == null

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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(Dimens.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceLarge)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClayIconButton(
                    onClick = {
                        if (state.hasUnsavedChanges) showDiscardDialog = true
                        else onNavigateBack()
                    },
                    elevation = ClayElevation.Raised,
                    contentDescription = stringResource(R.string.action_cancel)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }

                Text(
                    text = if (state.isEditing) stringResource(R.string.edit_book_title) else stringResource(R.string.add_book_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                ClayIconButton(
                    onClick = onSave,
                    enabled = isValid && !state.isSaving,
                    elevation = ClayElevation.Raised,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = stringResource(R.string.action_save)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                }
            }

            // Header Area: Clay Book Cover Placeholder (ready for v4 covers)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.spaceMedium),
                contentAlignment = Alignment.Center
            ) {
                BookCoverPlaceholder(
                    title = state.title.ifBlank { "New Book" },
                    author = state.author,
                    width = 88.dp,
                    height = 120.dp
                )
            }

            // Title Field (Required)
            ClayTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = stringResource(R.string.field_book_title_required),
                placeholder = stringResource(R.string.field_book_title_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            // Author Field
            ClayTextField(
                value = state.author,
                onValueChange = onAuthorChange,
                label = stringResource(R.string.field_author),
                placeholder = stringResource(R.string.field_author_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            // Total Pages Field
            ClayTextField(
                value = state.totalPagesInput,
                onValueChange = onTotalPagesChange,
                label = stringResource(R.string.field_total_pages_required),
                placeholder = "480",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                isError = validation == com.sniperoftdm.booktracker.domain.ValidationReason.TOTAL_PAGES_NOT_POSITIVE ||
                        validation == com.sniperoftdm.booktracker.domain.ValidationReason.TOTAL_PAGES_BELOW_CURRENT,
                errorMessage = validation?.toMessage()
            )

            // Starting Page Field (default 0)
            ClayTextField(
                value = state.startPageInput,
                onValueChange = onStartPageChange,
                label = stringResource(R.string.field_start_page),
                helperText = stringResource(R.string.helper_start_page),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                isError = validation == com.sniperoftdm.booktracker.domain.ValidationReason.START_PAGE_OUT_OF_RANGE,
                errorMessage = validation?.toMessage()
            )

            // Optional Goal Date Field with LIVE helper
            val liveHelper = state.requiredPaceLive?.let { pace ->
                stringResource(R.string.live_pace_helper_format, pace.toInt().coerceAtLeast(1))
            }
            DatePickerField(
                selectedDate = state.goalDate,
                onDateSelected = onGoalDateChange,
                label = stringResource(R.string.field_goal_date_optional),
                modifier = Modifier.fillMaxWidth()
            )
            if (liveHelper != null) {
                Text(
                    text = liveHelper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Dimens.spaceSmall, top = 2.dp)
                )
            }

            // Status Selector (Only when editing)
            if (state.isEditing) {
                Text(
                    text = stringResource(R.string.field_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val statusList = listOf(BookStatus.READING, BookStatus.PAUSED, BookStatus.FINISHED)
                val selectedIndex = statusList.indexOf(state.status).coerceAtLeast(0)

                ClaySegmentedControl(
                    items = statusList,
                    selectedIndex = selectedIndex,
                    onItemSelected = { onStatusChange(statusList[it]) }
                ) { status ->
                    Text(
                        when (status) {
                            BookStatus.READING -> stringResource(R.string.filter_reading)
                            BookStatus.PAUSED -> stringResource(R.string.filter_paused)
                            BookStatus.FINISHED -> stringResource(R.string.filter_finished)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spaceMedium))

            // Save Button
            ClayButton(
                onClick = onSave,
                enabled = isValid && !state.isSaving,
                variant = ClayButtonVariant.Filled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }

            Spacer(modifier = Modifier.height(Dimens.spaceHuge))
        }
    }

    // Discard Changes Confirmation Dialog
    if (showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.discard_changes_title),
            message = stringResource(R.string.discard_changes_message),
            confirmText = stringResource(R.string.action_discard),
            isDestructive = true,
            onConfirm = {
                showDiscardDialog = false
                onNavigateBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
}

@BookTrackerPreviews
@Composable
private fun AddEditBookPreview() {
    com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme {
        AddEditBookScreen(
            state = AddEditBookUiState(
                title = "Clean Architecture",
                author = "Robert C. Martin",
                totalPagesInput = "432",
                startPageInput = "0"
            ),
            onTitleChange = {},
            onAuthorChange = {},
            onTotalPagesChange = {},
            onStartPageChange = {},
            onGoalDateChange = {},
            onStatusChange = {},
            onSave = {},
            onNavigateBack = {}
        )
    }
}
