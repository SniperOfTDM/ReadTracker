package com.sniperoftdm.booktracker.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayChip
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.clay
import com.sniperoftdm.booktracker.ui.clay.clayClickable
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PageStepper(
    page: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(page) {
        mutableStateOf(TextFieldValue(page.toString(), TextRange(page.toString().length)))
    }
    val focusRequester = remember { FocusRequester() }

    fun updatePage(newPage: Int) {
        val clamped = if (totalPages > 0) newPage.coerceIn(0, totalPages) else newPage.coerceAtLeast(0)
        onPageChange(clamped)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
    ) {
        // Main Stepper Row: (-) [Page Display] (+)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrement Button with hold to repeat
            RepeatableStepButton(
                icon = { Icon(Icons.Rounded.Remove, contentDescription = "Decrease page") },
                onStep = { updatePage(page - 1) },
                enabled = page > 0
            )

            // Inset Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Dimens.spaceMedium)
                    .height(68.dp)
                    .clay(
                        shape = ClayShapes.card,
                        color = clayColors.face,
                        elevation = ClayElevation.Inset
                    )
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isEditing = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isEditing) {
                    BasicTextField(
                        value = editText,
                        onValueChange = {
                            editText = it
                            it.text.toIntOrNull()?.let { num ->
                                updatePage(num)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        keyboardActions = KeyboardActions(onDone = { isEditing = false }),
                        textStyle = BookTrackerTypography.tabularStatValue.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = page.toString(),
                            style = BookTrackerTypography.tabularStatValue.copy(fontSize = 32.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (totalPages > 0) {
                            Text(
                                text = "of $totalPages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Increment Button with hold to repeat
            RepeatableStepButton(
                icon = { Icon(Icons.Rounded.Add, contentDescription = "Increase page") },
                onStep = { updatePage(page + 1) },
                enabled = totalPages == 0 || page < totalPages
            )
        }

        // Quick Chips Row (+1, +5, +10, +25)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(1, 5, 10, 25).forEach { delta ->
                ClayChip(
                    selected = false,
                    onClick = { updatePage(page + delta) }
                ) {
                    Text("+$delta")
                }
            }
        }
    }
}

@Composable
private fun RepeatableStepButton(
    icon: @Composable () -> Unit,
    onStep: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .size(54.dp)
            .clay(
                shape = CircleShape,
                color = clayColors.face,
                elevation = if (enabled) ClayElevation.Raised else ClayElevation.Flat
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        onStep()
                        repeatJob = scope.launch {
                            delay(400) // Initial hold delay
                            while (true) {
                                onStep()
                                delay(120) // Accelerated repeat
                            }
                        }
                        tryAwaitRelease()
                        repeatJob?.cancel()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
