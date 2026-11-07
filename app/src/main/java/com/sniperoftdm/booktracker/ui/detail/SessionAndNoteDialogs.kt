package com.sniperoftdm.booktracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.model.Note
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayDialog
import com.sniperoftdm.booktracker.ui.clay.ClayTextField
import com.sniperoftdm.booktracker.ui.components.DatePickerField
import com.sniperoftdm.booktracker.ui.theme.Dimens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun EditSessionDialog(
    session: Session,
    totalPages: Int,
    onConfirm: (Session) -> Unit,
    onDismiss: () -> Unit
) {
    var pageText by remember { mutableStateOf(session.endPage.toString()) }
    val initialDate = remember(session.timestamp) {
        Instant.ofEpochMilli(session.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val pageNumber = pageText.toIntOrNull()
    val isValid = pageNumber != null && pageNumber in 1..totalPages

    ClayDialog(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.edit_session_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

        ClayTextField(
            value = pageText,
            onValueChange = { pageText = it.filter { char -> char.isDigit() } },
            label = stringResource(R.string.current_page_label),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

        DatePickerField(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            label = stringResource(R.string.checkpoint_date_label),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimens.spaceLarge))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            ClayButton(
                onClick = onDismiss,
                variant = ClayButtonVariant.OutlinedInset,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }

            Spacer(modifier = Modifier.width(Dimens.spaceSmall))

            ClayButton(
                onClick = {
                    if (pageNumber != null) {
                        val newTimestamp = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onConfirm(
                            session.copy(
                                endPage = pageNumber,
                                timestamp = newTimestamp
                            )
                        )
                    }
                },
                enabled = isValid,
                variant = ClayButtonVariant.Filled,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
fun EditNoteDialog(
    note: Note,
    totalPages: Int,
    onConfirm: (Note) -> Unit,
    onDismiss: () -> Unit
) {
    var pageText by remember { mutableStateOf(note.page.toString()) }
    var noteText by remember { mutableStateOf(note.text) }

    val pageNumber = pageText.toIntOrNull()
    val isValid = pageNumber != null && pageNumber in 0..totalPages && noteText.isNotBlank()

    ClayDialog(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.edit_note_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

        ClayTextField(
            value = pageText,
            onValueChange = { pageText = it.filter { char -> char.isDigit() } },
            label = stringResource(R.string.book_label),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

        ClayTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = stringResource(R.string.add_note_placeholder),
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimens.spaceLarge))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            ClayButton(
                onClick = onDismiss,
                variant = ClayButtonVariant.OutlinedInset,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }

            Spacer(modifier = Modifier.width(Dimens.spaceSmall))

            ClayButton(
                onClick = {
                    if (pageNumber != null) {
                        onConfirm(
                            note.copy(
                                page = pageNumber,
                                text = noteText.trim()
                            )
                        )
                    }
                },
                enabled = isValid,
                variant = ClayButtonVariant.Filled,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
