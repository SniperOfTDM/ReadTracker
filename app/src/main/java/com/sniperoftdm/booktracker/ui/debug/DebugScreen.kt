package com.sniperoftdm.booktracker.ui.debug

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sniperoftdm.booktracker.AppContainer
import com.sniperoftdm.booktracker.data.backup.ImportResult
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.ui.detail.BookDetailViewModel
import com.sniperoftdm.booktracker.ui.library.LibraryViewModel
import com.sniperoftdm.booktracker.ui.log.QuickLogEvent
import com.sniperoftdm.booktracker.ui.log.QuickLogViewModel
import com.sniperoftdm.booktracker.ui.notes.NotesViewModel
import com.sniperoftdm.booktracker.ui.settings.SettingsViewModel
import com.sniperoftdm.booktracker.ui.stats.StatsViewModel
import com.sniperoftdm.booktracker.worker.DailyReminderWorker
import com.sniperoftdm.booktracker.worker.ReminderScheduler
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val libraryViewModel = remember {
        LibraryViewModel(
            repository = container.repository,
            statsCalculator = container.statsCalculator,
            settingsRepository = container.settingsRepository
        )
    }
    val quickLogViewModel = remember {
        QuickLogViewModel(
            repository = container.repository,
            timerRepository = container.timerRepository
        )
    }
    val statsViewModel = remember {
        StatsViewModel(
            repository = container.repository,
            statsCalculator = container.statsCalculator,
            settingsRepository = container.settingsRepository
        )
    }
    val settingsViewModel = remember {
        SettingsViewModel(container.settingsRepository)
    }

    val libraryState by libraryViewModel.uiState.collectAsState()
    val quickLogState by quickLogViewModel.uiState.collectAsState()
    val statsState by statsViewModel.uiState.collectAsState()
    val userSettings by settingsViewModel.settings.collectAsState()

    var selectedBookDetailId by remember { mutableStateOf<Long?>(null) }
    val detailViewModel = remember(selectedBookDetailId) {
        selectedBookDetailId?.let { id ->
            BookDetailViewModel(
                bookId = id,
                repository = container.repository,
                statsCalculator = container.statsCalculator,
                settingsRepository = container.settingsRepository
            )
        }
    }
    val detailState = detailViewModel?.uiState?.collectAsState()?.value

    val notesViewModel = remember(selectedBookDetailId) {
        selectedBookDetailId?.let { id ->
            NotesViewModel(bookId = id, repository = container.repository)
        }
    }
    val notesState = notesViewModel?.uiState?.collectAsState()?.value

    // Storage Access Framework contracts for Backup Export and Import
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        container.backupRepository.export(stream)
                    }
                    snackbarHostState.showSnackbar("Backup exported successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val result = context.contentResolver.openInputStream(uri)?.use { stream ->
                        container.backupRepository.import(stream)
                    }
                    when (result) {
                        is ImportResult.Success -> {
                            snackbarHostState.showSnackbar("Imported ${result.booksImported} books, ${result.sessionsImported} checkpoints, ${result.notesImported} notes")
                        }
                        is ImportResult.ValidationError -> {
                            val issues = result.report.problems.joinToString("; ") { "${it.bookTitle}: ${it.reason}" }
                            snackbarHostState.showSnackbar("Validation failed: $issues")
                        }
                        is ImportResult.Error -> {
                            snackbarHostState.showSnackbar("Import error: ${result.message}")
                        }
                        null -> snackbarHostState.showSnackbar("Unable to open backup file")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Import failed: ${e.message}")
                }
            }
        }
    }

    // Permission request launcher for notifications on Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            settingsViewModel.setReminderEnabled(true)
            coroutineScope.launch {
                ReminderScheduler.schedule(context)
                snackbarHostState.showSnackbar("Notifications enabled and reminder scheduled")
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Notification permission denied")
            }
        }
    }

    LaunchedEffect(Unit) {
        libraryViewModel.errorEvents.collect { reason ->
            snackbarHostState.showSnackbar("Library error: $reason")
        }
    }

    LaunchedEffect(Unit) {
        quickLogViewModel.events.collect { event ->
            when (event) {
                is QuickLogEvent.Saved -> snackbarHostState.showSnackbar("Checkpoint logged successfully!")
                is QuickLogEvent.Error -> snackbarHostState.showSnackbar("Validation error: ${event.reason}")
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("BookTracker Debug Screen (v2)") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Controls & Demo Data
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Controls & Demo Data", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    SeedData.seed(container.repository)
                                    snackbarHostState.showSnackbar("Demo data seeded (3 books, 3 weeks)")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Seed demo data")
                        }
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    SeedData.clearAll(container.repository)
                                    selectedBookDetailId = null
                                    snackbarHostState.showSnackbar("All data cleared")
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear all data")
                        }
                    }
                }
            }

            // Backup Export & Import Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Backup & Restore (JSON SAF)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportLauncher.launch("booktracker_backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export Backup")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import Backup")
                        }
                    }
                }
            }

            // Global Statistics & Extra Stats
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Global Statistics & Projections",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total Pages Read: ${statsState.globalStats.totalPagesRead}")
                    Text("Current Streak: ${statsState.globalStats.currentStreak} days")
                    Text("Longest Streak: ${statsState.globalStats.longestStreak} days")
                    val globalPph = statsState.globalStats.pagesPerHour
                    Text("Global Reading Speed: ${if (globalPph != null) String.format("%.1f p/h", globalPph) else "No timed sessions"}")

                    val best = statsState.globalStats.bestDay
                    if (best != null) {
                        Text("Best Day: ${best.first} (${best.second} pages)")
                    } else {
                        Text("Best Day: None yet")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    val yearlyGoal = statsState.yearlyGoalProgress
                    if (yearlyGoal != null) {
                        Text("Yearly Goal: ${yearlyGoal.finishedCount} / ${yearlyGoal.goal} books (${(yearlyGoal.progressFraction * 100).toInt()}%)")
                        Text("Pace Status: ${yearlyGoal.aheadBehindSchedule} books vs schedule")
                    } else {
                        Text("Yearly Goal: Off (set target in Settings)")
                    }
                    Text("Projected Books by Dec 31: ${statsState.projectedBooksByYearEnd}")

                    val totalEta = statsState.totalEta
                    if (totalEta != null) {
                        Text("Total Reading ETA: ${totalEta.daysLeft} days (${totalEta.finishDate}) @ ${String.format("%.1f", totalEta.sharedPace)} p/d")
                    } else {
                        Text("Total Reading ETA: No active pace across reading books")
                    }
                }
            }

            // Settings & Reminder Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Settings (DataStore)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Count zero-reading days in average")
                        Switch(
                            checked = userSettings.countZeroDays,
                            onCheckedChange = { settingsViewModel.setCountZeroDays(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pace window: ${userSettings.paceWindowDays} days")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(onClick = { settingsViewModel.setPaceWindowDays(userSettings.paceWindowDays - 1) }) { Text("-") }
                            FilledTonalButton(onClick = { settingsViewModel.setPaceWindowDays(userSettings.paceWindowDays + 1) }) { Text("+") }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Yearly goal: ${userSettings.yearlyGoalBooks} books")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(onClick = { settingsViewModel.setYearlyGoalBooks((userSettings.yearlyGoalBooks - 1).coerceAtLeast(0)) }) { Text("-") }
                            FilledTonalButton(onClick = { settingsViewModel.setYearlyGoalBooks(userSettings.yearlyGoalBooks + 1) }) { Text("+") }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Reminder (${String.format("%02d:%02d", userSettings.reminderHour, userSettings.reminderMinute)})")
                        Switch(
                            checked = userSettings.reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    settingsViewModel.setReminderEnabled(enabled)
                                    coroutineScope.launch {
                                        if (enabled) {
                                            ReminderScheduler.schedule(context)
                                        } else {
                                            ReminderScheduler.cancel(context)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val testRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>().build()
                                WorkManager.getInstance(context).enqueue(testRequest)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Fired test reminder work immediately")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fire test notification now")
                        }
                    }
                }
            }

            // Session Timer Section
            val activeTimer = quickLogState.activeTimer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (activeTimer != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Reading Session Timer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (activeTimer != null) {
                        val book = quickLogState.books.find { it.book.id == activeTimer.bookId }?.book
                        val elapsed = ((System.currentTimeMillis() - activeTimer.startedAtMillis) / 1000).coerceAtLeast(0)
                        Text("Running for: ${book?.title ?: "Book #${activeTimer.bookId}"}")
                        Text("Elapsed: ${elapsed / 60}m ${elapsed % 60}s")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { quickLogViewModel.cancelTimer() }) {
                                Text("Cancel Timer")
                            }
                        }
                    } else {
                        Text("No active timer. Select a book below and tap 'Start Timer'.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            // Add Book Section
            AddBookSection(onAddBook = { title, author, totalPages, startPage, goalDate ->
                libraryViewModel.addBook(
                    title = title,
                    author = author,
                    totalPages = totalPages,
                    startPage = startPage,
                    goalDateEpochDay = goalDate?.toEpochDay()
                )
            })

            // Log Checkpoint Section
            LogCheckpointSection(
                books = quickLogState.books.map { it.book },
                selectedBookId = quickLogState.selectedBookId,
                pageInput = quickLogState.pageInput,
                onBookSelected = { quickLogViewModel.selectBook(it) },
                onPageChanged = { quickLogViewModel.onPageChanged(it) },
                onStepPage = { quickLogViewModel.stepPage(it) },
                onStartTimer = { quickLogViewModel.startTimer() },
                onSubmit = { quickLogViewModel.submit() }
            )

            // Book List with Stats
            Text(
                "Books in Library (${libraryState.books.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "All", BookStatus.READING to "Reading", BookStatus.PAUSED to "Paused", BookStatus.FINISHED to "Finished")
                    .forEach { (status, label) ->
                        val selected = libraryState.selectedFilter == status
                        Button(
                            onClick = { libraryViewModel.setFilter(status) },
                            colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(label)
                        }
                    }
            }

            libraryState.books.forEach { item ->
                val book = item.book
                val stats = item.stats
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                if (book.author.isNotBlank()) {
                                    Text("by ${book.author}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            Text(
                                book.status.name,
                                fontWeight = FontWeight.SemiBold,
                                color = when (book.status) {
                                    BookStatus.FINISHED -> Color(0xFF2E7D32)
                                    BookStatus.READING -> Color(0xFF1565C0)
                                    BookStatus.PAUSED -> Color(0xFFE65100)
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text("Current page: ${stats.currentPage} / ${book.totalPages} (${(stats.progressPercent * 100).toInt()}%)")
                        Text("Pages left: ${stats.pagesLeft}")
                        Text("Today's pages: ${stats.todayPages}")
                        Text("Daily avg (all-time): ${String.format("%.1f", stats.dailyAverageAllTime)}")
                        Text("Daily avg (last ${userSettings.paceWindowDays}d): ${String.format("%.1f", stats.dailyAverageRecent)}")
                        val pph = stats.pagesPerHour
                        Text("Speed: ${if (pph != null) String.format("%.1f p/h", pph) else "No timed sessions"}")

                        if (stats.estimatedDaysLeft != null) {
                            Text("Estimated days left: ${stats.estimatedDaysLeft} (${stats.estimatedFinishDate})")
                        } else {
                            Text("Estimated finish: No active pace")
                        }

                        if (book.goalDateEpochDay != null) {
                            val goalDate = LocalDate.ofEpochDay(book.goalDateEpochDay)
                            Text("Goal date: $goalDate")
                            Text("Required pages/day: ${stats.requiredPagesPerDay ?: 0}")
                            Text(
                                "On track: ${if (stats.isOnTrack == true) "YES" else "NO"}",
                                fontWeight = FontWeight.Bold,
                                color = if (stats.isOnTrack == true) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedBookDetailId = if (selectedBookDetailId == book.id) null else book.id
                                }
                            ) {
                                Text(if (selectedBookDetailId == book.id) "Hide Details" else "View Details & Notes")
                            }
                            TextButton(
                                onClick = { libraryViewModel.deleteBook(book) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        }

                        // Expanded Sessions & Notes Section
                        if (selectedBookDetailId == book.id && detailState != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Notes subsection
                            Text("Book Notes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            var notePageInput by remember { mutableStateOf("") }
                            var noteTextInput by remember { mutableStateOf("") }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = notePageInput,
                                    onValueChange = { notePageInput = it },
                                    label = { Text("Page") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(90.dp)
                                )
                                OutlinedTextField(
                                    value = noteTextInput,
                                    onValueChange = { noteTextInput = it },
                                    label = { Text("Note text") },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val page = notePageInput.toIntOrNull() ?: 0
                                        notesViewModel?.addNote(page, noteTextInput)
                                        noteTextInput = ""
                                    }
                                ) {
                                    Text("Add")
                                }
                            }

                            val currentNotes = notesState?.notes ?: emptyList()
                            if (currentNotes.isEmpty()) {
                                Text("No notes added yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            } else {
                                currentNotes.forEach { note ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("p. ${note.page}: ${note.text}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        TextButton(onClick = { notesViewModel?.deleteNote(note) }) {
                                            Text("Delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Sessions history subsection
                            Text("Session History (Grouped by Day)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            if (detailState.dayGroups.isEmpty()) {
                                Text("No checkpoints logged yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            } else {
                                detailState.dayGroups.forEach { dayGroup ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "${dayGroup.dayLabel} — ${dayGroup.date}",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    dayGroup.sessions.forEach { (session, pagesRead) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val durationText = session.durationSeconds?.let { " (${it / 60}m)" } ?: ""
                                            Text(
                                                "Page ${session.endPage} (+$pagesRead pages$durationText)",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            TextButton(
                                                onClick = { detailViewModel.deleteSession(session) }
                                            ) {
                                                Text("Delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AddBookSection(
    onAddBook: (title: String, author: String, totalPages: Int, startPage: Int, goalDate: LocalDate?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf("") }
    var startPage by remember { mutableStateOf("0") }
    var goalDateStr by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Add New Book", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = totalPages,
                    onValueChange = { totalPages = it },
                    label = { Text("Total Pages *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startPage,
                    onValueChange = { startPage = it },
                    label = { Text("Start Page") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = goalDateStr,
                onValueChange = { goalDateStr = it },
                label = { Text("Goal Date YYYY-MM-DD (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val total = totalPages.toIntOrNull() ?: 0
                    val start = startPage.toIntOrNull() ?: 0
                    val parsedDate = if (goalDateStr.isNotBlank()) {
                        try {
                            LocalDate.parse(goalDateStr.trim())
                        } catch (e: DateTimeParseException) {
                            null
                        }
                    } else null

                    onAddBook(title, author, total, start, parsedDate)
                    title = ""
                    author = ""
                    totalPages = ""
                    startPage = "0"
                    goalDateStr = ""
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Add Book")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogCheckpointSection(
    books: List<com.sniperoftdm.booktracker.data.model.Book>,
    selectedBookId: Long?,
    pageInput: String,
    onBookSelected: (Long) -> Unit,
    onPageChanged: (String) -> Unit,
    onStepPage: (Int) -> Unit,
    onStartTimer: () -> Unit,
    onSubmit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedBook = books.find { it.id == selectedBookId }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Log Checkpoint", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (books.isEmpty()) {
                Text("Add a book above to start logging checkpoints.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedBook?.title ?: "Select Book",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        books.forEach { book ->
                            DropdownMenuItem(
                                text = { Text(book.title) },
                                onClick = {
                                    onBookSelected(book.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = { onStepPage(-1) }) { Text("-1") }
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = onPageChanged,
                        label = { Text("Page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(onClick = { onStepPage(1) }) { Text("+1") }
                    Button(onClick = onSubmit) {
                        Text("Save")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onStartTimer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Timer For This Book")
                }
            }
        }
    }
}
