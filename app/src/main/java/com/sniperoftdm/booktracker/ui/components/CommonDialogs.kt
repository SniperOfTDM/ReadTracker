package com.sniperoftdm.booktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayDialog
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayTextField
import com.sniperoftdm.booktracker.ui.clay.clayClickable
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.action_confirm),
    dismissText: String = stringResource(R.string.action_cancel),
    isDestructive: Boolean = false
) {
    ClayDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text(dismissText)
            }

            Spacer(modifier = Modifier.width(Dimens.spaceSmall))

            ClayButton(
                onClick = onConfirm,
                variant = ClayButtonVariant.Filled,
                modifier = Modifier.height(48.dp)
            ) {
                Text(confirmText)
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spaceSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        action?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    val formattedDate = selectedDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        ?: stringResource(R.string.not_set)

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        ClayTextField(
            value = formattedDate,
            onValueChange = {},
            readOnly = true,
            label = label,
            modifier = Modifier.clayClickable(
                shape = ClayShapes.textField,
                color = LocalClayColors.current.face,
                elevation = ClayElevation.Inset,
                onClick = { showDialog = true }
            )
        )
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            onDateSelected(localDate)
                        } else {
                            onDateSelected(null)
                        }
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = true
    )

    val formattedTime = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute)

    Box(modifier = modifier) {
        ClayTextField(
            value = formattedTime,
            onValueChange = {},
            readOnly = true,
            label = label,
            modifier = Modifier.clayClickable(
                shape = ClayShapes.textField,
                color = LocalClayColors.current.face,
                elevation = ClayElevation.Inset,
                onClick = { showDialog = true }
            )
        )
    }

    if (showDialog) {
        ClayDialog(onDismissRequest = { showDialog = false }) {
            Text(label, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Dimens.spaceMedium))
            TimePicker(state = timePickerState)
            Spacer(modifier = Modifier.height(Dimens.spaceLarge))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ClayButton(
                    onClick = { showDialog = false },
                    variant = ClayButtonVariant.OutlinedInset,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(Dimens.spaceSmall))
                ClayButton(
                    onClick = {
                        onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showDialog = false
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
