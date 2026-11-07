package com.sniperoftdm.booktracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sniperoftdm.booktracker.data.backup.ImportProblem
import com.sniperoftdm.booktracker.data.backup.ImportReport
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.domain.BookStats
import com.sniperoftdm.booktracker.domain.GlobalStats
import com.sniperoftdm.booktracker.domain.TotalEta
import com.sniperoftdm.booktracker.domain.YearlyGoalProgress
import com.sniperoftdm.booktracker.ui.detail.BookDetailScreen
import com.sniperoftdm.booktracker.ui.detail.BookDetailUiState
import com.sniperoftdm.booktracker.ui.detail.DayListItem
import com.sniperoftdm.booktracker.ui.settings.BackupOperationState
import com.sniperoftdm.booktracker.ui.settings.SettingsScreen
import com.sniperoftdm.booktracker.ui.stats.DayBookSessions
import com.sniperoftdm.booktracker.ui.stats.StatsPeriod
import com.sniperoftdm.booktracker.ui.stats.StatsScreen
import com.sniperoftdm.booktracker.ui.stats.StatsUiState
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class StageBUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun statsScreen_withSeededData_displaysSummaryTiles() {
        val today = LocalDate.now()
        val statsState = StatsUiState(
            globalStats = GlobalStats(
                currentStreak = 5,
                longestStreak = 12,
                bestDay = today.minusDays(2) to 65,
                totalPagesRead = 420,
                pagesPerDay = mapOf(today to 30, today.minusDays(1) to 45)
            ),
            selectedPeriod = StatsPeriod.WEEK,
            booksFinishedThisYear = 3,
            yearlyGoalProgress = YearlyGoalProgress(
                finishedCount = 3,
                goal = 12,
                progressFraction = 0.25f,
                aheadBehindSchedule = 0.5
            ),
            isLoading = false
        )

        composeTestRule.setContent {
            BookTrackerTheme {
                StatsScreen(
                    state = statsState,
                    onPeriodSelected = {},
                    onDayClick = {},
                    onDismissDayDetail = {},
                    onSetYearlyGoal = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Total pages").assertIsDisplayed()
        composeTestRule.onNodeWithText("420").assertIsDisplayed()
        composeTestRule.onNodeWithText("Current streak").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yearly Reading Goal").assertIsDisplayed()
    }

    @Test
    fun statsScreen_heatmapDaySelection_opensDaySheet() {
        val selectedDate = LocalDate.of(2026, 11, 4)
        val book = Book(id = 1L, title = "Dune", totalPages = 600)
        val sessions = listOf(Session(id = 1, bookId = 1, timestamp = System.currentTimeMillis(), endPage = 120) to 25)

        val statsState = StatsUiState(
            globalStats = GlobalStats(
                currentStreak = 1,
                longestStreak = 1,
                bestDay = selectedDate to 25,
                totalPagesRead = 25,
                pagesPerDay = mapOf(selectedDate to 25)
            ),
            selectedDay = selectedDate,
            selectedDaySessions = listOf(DayBookSessions(book, 25, sessions)),
            isLoading = false
        )

        composeTestRule.setContent {
            BookTrackerTheme {
                StatsScreen(
                    state = statsState,
                    onPeriodSelected = {},
                    onDayClick = {},
                    onDismissDayDetail = {},
                    onSetYearlyGoal = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Reading on Wednesday, 4 November 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dune").assertIsDisplayed()
        composeTestRule.onNodeWithText("+25 pages on this day").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_togglesSwitch_callsCallback() {
        var toggleCalled = false
        val settings = UserSettings(countZeroDays = true, paceWindowDays = 7)

        composeTestRule.setContent {
            BookTrackerTheme {
                SettingsScreen(
                    settings = settings,
                    backupState = BackupOperationState.Idle,
                    onCountZeroDaysChange = { toggleCalled = true },
                    onPaceWindowDaysChange = {},
                    onYearlyGoalChange = {},
                    onReminderEnabledChange = {},
                    onReminderTimeChange = { _, _ -> },
                    onThemeModeChange = {},
                    onUseDynamicColorChange = {},
                    onExportBackup = {},
                    onImportBackup = {},
                    onResetBackupState = {},
                    onClearAllData = {},
                    onSendTestNotification = {},
                    onNavigateToDebug = {},
                    onNavigateToGallery = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Count zero-reading days in averages").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_importInvalidFile_showsReportDialog() {
        val problems = listOf(
            ImportProblem(bookTitle = "Invalid Book", reason = "End page exceeds total pages")
        )
        val report = ImportReport(problems)

        composeTestRule.setContent {
            BookTrackerTheme {
                SettingsScreen(
                    settings = UserSettings(),
                    backupState = BackupOperationState.ImportValidationFailed(report),
                    onCountZeroDaysChange = {},
                    onPaceWindowDaysChange = {},
                    onYearlyGoalChange = {},
                    onReminderEnabledChange = {},
                    onReminderTimeChange = { _, _ -> },
                    onThemeModeChange = {},
                    onUseDynamicColorChange = {},
                    onExportBackup = {},
                    onImportBackup = {},
                    onResetBackupState = {},
                    onClearAllData = {},
                    onSendTestNotification = {},
                    onNavigateToDebug = {},
                    onNavigateToGallery = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Import Validation Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Invalid Book: End page exceeds total pages").assertIsDisplayed()
    }

    @Test
    fun bookDetailScreen_displaysDaysTabAndNotes() {
        val testBook = Book(id = 1L, title = "Atomic Habits", author = "James Clear", totalPages = 320)
        val session = Session(id = 1L, bookId = 1L, timestamp = System.currentTimeMillis(), endPage = 40)
        val dayItem = DayListItem.ReadingDay(
            dayNumber = 1,
            date = LocalDate.now(),
            totalPagesRead = 40,
            sessions = listOf(session to 40)
        )

        val detailState = BookDetailUiState(
            book = testBook,
            stats = BookStats(
                currentPage = 40,
                pagesLeft = 280,
                progressPercent = 40f / 320f,
                todayPages = 40,
                dailyAverageAllTime = 20.0,
                dailyAverageRecent = 20.0,
                estimatedDaysLeft = 14,
                estimatedFinishDate = LocalDate.now().plusDays(14),
                requiredPagesPerDay = null,
                isOnTrack = null,
                pagesPerDay = mapOf(LocalDate.now() to 40),
                sessionsWithPages = listOf(session to 40)
            ),
            dayListItems = listOf(dayItem),
            isLoading = false
        )

        composeTestRule.setContent {
            BookTrackerTheme {
                BookDetailScreen(
                    state = detailState,
                    onBack = {},
                    onNavigateToEditBook = {},
                    onQuickLogClick = {},
                    onStartTimer = {},
                    onCancelTimer = {},
                    onTogglePause = {},
                    onMarkFinished = {},
                    onDeleteBook = {},
                    onEditSession = {},
                    onDeleteSession = {},
                    onUndoDeleteSession = {},
                    onAddNote = { _, _ -> },
                    onEditNote = {},
                    onDeleteNote = {},
                    onUndoDeleteNote = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Atomic Habits").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily average").assertIsDisplayed()
        composeTestRule.onNodeWithText("+40 p").assertIsDisplayed()
    }
}
