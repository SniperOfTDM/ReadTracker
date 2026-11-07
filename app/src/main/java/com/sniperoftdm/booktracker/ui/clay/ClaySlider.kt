package com.sniperoftdm.booktracker.ui.clay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun ClaySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true
) {
    val clayColors = LocalClayColors.current
    val thumbSize = 28.dp
    val trackHeight = 12.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbSize)
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                    onValueChange(newValue)
                }
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                    onValueChange(newValue)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        val availableWidth = maxWidth - thumbSize

        // Inset track groove
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clay(
                    shape = ClayShapes.pill,
                    color = clayColors.face,
                    elevation = ClayElevation.Inset
                )
        )

        // Active fill
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(trackHeight)
                .clay(
                    shape = ClayShapes.pill,
                    color = MaterialTheme.colorScheme.primary,
                    elevation = ClayElevation.Raised
                )
        )

        // Raised round thumb
        Box(
            modifier = Modifier
                .offset(x = availableWidth * fraction)
                .size(thumbSize)
                .clay(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    elevation = ClayElevation.Raised
                )
        )
    }
}
