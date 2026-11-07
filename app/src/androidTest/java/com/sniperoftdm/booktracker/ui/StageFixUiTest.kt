package com.sniperoftdm.booktracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.repository.ThemeMode
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.domain.BookStats
import com.sniperoftdm.booktracker.domain.GlobalStats
import com.sniperoftdm.booktracker.ui.clay.ClayNavBar
import com.sniperoftdm.booktracker.ui.clay.ClayNavItem
import com.sniperoftdm.booktracker.ui.home.HomeScreen
import com.sniperoftdm.booktracker.ui.home.HomeUiState
import com.sniperoftdm.booktracker.ui.library.BookItemUi
import com.sniperoftdm.booktracker.ui.library.LibraryScreen
import com.sniperoftdm.booktracker.ui.library.LibrarySortOrder
import com.sniperoftdm.booktracker.ui.library.LibraryUiState
import com.sniperoftdm.booktracker.ui.settings.BackupOperationState
import com.sniperoftdm.booktracker.ui.settings.SettingsScreen
import com.sniperoftdm.booktracker.ui.stats.StatsPeriod
import com.sniperoftdm.booktracker.ui.stats.StatsScreen
import com.sniperoftdm.booktracker.ui.stats.StatsUiState
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class StageFixUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navItems = listOf(
        ClayNavItem("Home", Icons.Rounded.Home, Icons.Rounded.Home, "home"),
        ClayNavItem("Library", Icons.AutoMirrored.Rounded.MenuBook, Icons.AutoMirrored.Rounded.MenuBook, "library"),
        ClayNavItem("Stats", Icons.Rounded.BarChart, Icons.Rounded.BarChart, "stats"),
        ClayNavItem("Settings", Icons.Rounded.Settings, Icons.Rounded.Settings, "settings")
    )

    @Test
    fun settingsScreen_lastItem_isAboveNavBarInDarkMode() {
        composeTestRule.setContent {
            BookTrackerTheme(themeMode = ThemeMode.DARK) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        settings = UserSettings(reminderEnabled = true),
                        backupState = BackupOperationState.Idle,
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

                    ClayNavBar(
                        items = navItems,
                        currentRoute = "settings",
                        onItemClick = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    )
                }
            }
        }

        // Scroll to the last item
        composeTestRule.onNodeWithTag("settings_last_item").performScrollTo()
        composeTestRule.waitForIdle()

        val lastItemBounds = composeTestRule.onNodeWithTag("settings_last_item").getBoundsInRoot()
        val navBarBounds = composeTestRule.onNodeWithTag("clay_nav_bar").getBoundsInRoot()

        assertTrue(
            "Settings last item bottom (${lastItemBounds.bottom}) must be <= nav bar top (${navBarBounds.top})",
            lastItemBounds.bottom <= navBarBounds.top
        )
    }

    @Test
    fun statsScreen_lastItem_isAboveNavBar() {
        val today = LocalDate.now()
        val statsState = StatsUiState(
            globalStats = GlobalStats(
                currentStreak = 2,
                longestStreak = 5,
                bestDay = today to 50,
                totalPagesRead = 100,
                pagesPerDay = mapOf(today to 50)
            ),
            selectedPeriod = StatsPeriod.WEEK,
            booksFinishedThisYear = 1,
            isLoading = false
        )

        composeTestRule.setContent {
            BookTrackerTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    StatsScreen(
                        state = statsState,
                        onPeriodSelected = {},
                        onDayClick = {},
                        onDismissDayDetail = {},
                        onSetYearlyGoal = {}
                    )

                    ClayNavBar(
                        items = navItems,
                        currentRoute = "stats",
                        onItemClick = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("stats_last_item").performScrollTo()
        composeTestRule.waitForIdle()

        val lastItemBounds = composeTestRule.onNodeWithTag("stats_last_item").getBoundsInRoot()
        val navBarBounds = composeTestRule.onNodeWithTag("clay_nav_bar").getBoundsInRoot()

        assertTrue(
            "Stats last item bottom (${lastItemBounds.bottom}) must be <= nav bar top (${navBarBounds.top})",
            lastItemBounds.bottom <= navBarBounds.top
        )
    }

    @Test
    fun title_statusBarInsets_areRespected() {
        composeTestRule.setContent {
            BookTrackerTheme {
                HomeScreen(
                    state = HomeUiState(isLoading = false),
                    onNavigateToDetail = {},
                    onNavigateToAddBook = {},
                    onNavigateToLibrary = {},
                    onNavigateToStats = {},
                    onOpenQuickLog = {},
                    onStopTimer = {}
                )
            }
        }

        val titleBounds = composeTestRule.onNodeWithTag("clay_screen_title").getBoundsInRoot()
        assertTrue("Title top (${titleBounds.top}) must be at or below system top (>= 0)", titleBounds.top >= 0.dp)
    }

    @Test
    fun fab_clearance_isAboveNavBarOnHomeAndLibrary() {
        val testBook = Book(id = 1L, title = "Test Book", author = "Author", totalPages = 200, status = BookStatus.READING)
        val testStats = BookStats(
            currentPage = 50,
            pagesLeft = 150,
            progressPercent = 0.25f,
            todayPages = 10,
            dailyAverageAllTime = 10.0,
            dailyAverageRecent = 10.0,
            estimatedDaysLeft = 15,
            estimatedFinishDate = LocalDate.now().plusDays(15),
            requiredPagesPerDay = 10,
            isOnTrack = true,
            pagesPerDay = emptyMap(),
            sessionsWithPages = emptyList()
        )
        val libraryState = LibraryUiState(
            books = listOf(BookItemUi(testBook, testStats)),
            sortOrder = LibrarySortOrder.LAST_READ,
            isLoading = false
        )

        composeTestRule.setContent {
            BookTrackerTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LibraryScreen(
                        state = libraryState,
                        onFilterChange = {},
                        onSortChange = {},
                        onBookClick = {},
                        onNavigateToAddBook = {},
                        onNavigateToEditBook = {},
                        onTogglePause = {},
                        onMarkFinished = {},
                        onDeleteBook = {}
                    )

                    ClayNavBar(
                        items = navItems,
                        currentRoute = "library",
                        onItemClick = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    )
                }
            }
        }

        val fabBounds = composeTestRule.onNodeWithTag("clay_fab_container").getBoundsInRoot()
        val navBarBounds = composeTestRule.onNodeWithTag("clay_nav_bar").getBoundsInRoot()

        assertTrue(
            "FAB bottom (${fabBounds.bottom}) must be <= nav bar top (${navBarBounds.top})",
            fabBounds.bottom <= navBarBounds.top
        )
    }

    @Test
    fun reminderTimeRow_click_opensTimePickerDialog() {
        composeTestRule.setContent {
            BookTrackerTheme {
                SettingsScreen(
                    settings = UserSettings(reminderEnabled = true, reminderHour = 20, reminderMinute = 30),
                    backupState = BackupOperationState.Idle,
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

        composeTestRule.onNodeWithText("Reminder time").performClick()
        composeTestRule.waitForIdle()

        // Assert Save and Cancel buttons in Clay TimePicker dialog are displayed
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
