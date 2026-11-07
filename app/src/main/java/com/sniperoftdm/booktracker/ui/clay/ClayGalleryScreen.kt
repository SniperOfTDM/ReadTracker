package com.sniperoftdm.booktracker.ui.clay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.BookTrackerPreviews
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun ClayGalleryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    var textInput by remember { mutableStateOf("Harry Potter") }
    var textErrorInput by remember { mutableStateOf("Negative page") }
    var switchState by remember { mutableStateOf(true) }
    var checkboxState by remember { mutableStateOf(true) }
    var sliderValue by remember { mutableFloatStateOf(0.65f) }
    var selectedChipIndex by remember { mutableIntStateOf(0) }
    var selectedSegmentIndex by remember { mutableIntStateOf(1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.spaceLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLarge)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clay Component Gallery",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            ClayButton(
                onClick = onBack,
                variant = ClayButtonVariant.OutlinedInset,
                modifier = Modifier.height(40.dp)
            ) {
                Text("Close")
            }
        }

        // 1. Cards and Elevations
        Text("1. Cards & Elevations", style = MaterialTheme.typography.titleLarge)
        ClayCard(elevation = ClayElevation.Flat) {
            Text("Flat Elevation (List row style)", style = MaterialTheme.typography.bodyMedium)
        }
        ClayCard(elevation = ClayElevation.Raised) {
            Text("Raised Elevation (Standard Card)", style = MaterialTheme.typography.bodyMedium)
        }
        ClayCard(elevation = ClayElevation.Floating) {
            Text("Floating Elevation (FAB/Sheet/Bar style)", style = MaterialTheme.typography.bodyMedium)
        }
        ClayCard(elevation = ClayElevation.Inset) {
            Text("Inset Elevation (Groove / Pressed style)", style = MaterialTheme.typography.bodyMedium)
        }

        ClayDivider()

        // 2. Buttons
        Text("2. Buttons", style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
        ) {
            ClayButton(
                onClick = {},
                variant = ClayButtonVariant.Filled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Filled Primary")
            }
            ClayButton(
                onClick = {},
                variant = ClayButtonVariant.Tonal,
                modifier = Modifier.weight(1f)
            ) {
                Text("Tonal Sage")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
        ) {
            ClayButton(
                onClick = {},
                variant = ClayButtonVariant.OutlinedInset,
                modifier = Modifier.weight(1f)
            ) {
                Text("Outlined Inset")
            }
            ClayButton(
                onClick = {},
                enabled = false,
                variant = ClayButtonVariant.Filled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Disabled")
            }
        }

        // Icon buttons & Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClayIconButton(onClick = {}) {
                Icon(Icons.Rounded.Favorite, contentDescription = "Favorite")
            }
            ClayIconButton(onClick = {}, color = clayColors.terracotta) {
                Icon(Icons.Rounded.Bookmark, contentDescription = "Bookmark")
            }
            ClayBadge {
                Text("Day 12")
            }
            ClayBadge(color = clayColors.sage) {
                Text("Finished")
            }
        }

        ClayDivider()

        // 3. Chips & Segmented Control
        Text("3. Chips & Segments", style = MaterialTheme.typography.titleLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("Reading (3)", "Paused (1)", "Finished (5)", "All (9)").forEachIndexed { index, title ->
                ClayChip(
                    selected = selectedChipIndex == index,
                    onClick = { selectedChipIndex = index }
                ) {
                    Text(title)
                }
            }
        }

        ClaySegmentedControl(
            items = listOf("Week", "Month", "All Time"),
            selectedIndex = selectedSegmentIndex,
            onItemSelected = { selectedSegmentIndex = it }
        ) { label ->
            Text(label)
        }

        ClayDivider()

        // 4. TextFields
        Text("4. Text Fields", style = MaterialTheme.typography.titleLarge)
        ClayTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = "Book Title",
            placeholder = "Enter book title..."
        )
        ClayTextField(
            value = textErrorInput,
            onValueChange = { textErrorInput = it },
            label = "Page Checkpoint",
            isError = true,
            errorMessage = "End page cannot be lower than current page 240"
        )

        ClayDivider()

        // 5. Switches, Checkboxes, Sliders
        Text("5. Toggles & Sliders", style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Count zero days in averages", style = MaterialTheme.typography.bodyMedium)
            ClaySwitch(checked = switchState, onCheckedChange = { switchState = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notify daily at 21:00", style = MaterialTheme.typography.bodyMedium)
            ClayCheckbox(checked = checkboxState, onCheckedChange = { checkboxState = it })
        }
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
            Text("Pace Window: ${(sliderValue * 27 + 3).toInt()} days", style = MaterialTheme.typography.bodyMedium)
            ClaySlider(value = sliderValue, onValueChange = { sliderValue = it })
        }

        ClayDivider()

        // 6. Progress Bar and Ring
        Text("6. Progress Indicators", style = MaterialTheme.typography.titleLarge)
        ClayProgressBar(progress = 0.65f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ClayProgressRing(
                progress = 0.65f,
                modifier = Modifier.size(120.dp)
            ) {
                Text("65%", style = MaterialTheme.typography.titleLarge)
            }
        }

        ClayDivider()

        // 7. FABs
        Text("7. Floating Action Buttons", style = MaterialTheme.typography.titleLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClayFab(
                onClick = {},
                icon = { Icon(Icons.Rounded.Add, contentDescription = "Add") }
            )
            ClayFab(
                onClick = {},
                expanded = true,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Log Pages") }
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spaceHuge))
    }
}

@BookTrackerPreviews
@Composable
private fun ClayGalleryPreview() {
    com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme {
        ClayGalleryScreen(onBack = {})
    }
}
