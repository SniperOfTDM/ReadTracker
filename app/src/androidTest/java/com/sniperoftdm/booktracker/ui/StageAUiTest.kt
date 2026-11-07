package com.sniperoftdm.booktracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.domain.BookStats
import com.sniperoftdm.booktracker.ui.book.AddEditBookScreen
import com.sniperoftdm.booktracker.ui.book.AddEditBookUiState
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.home.HomeScreen
import com.sniperoftdm.booktracker.ui.home.HomeUiState
import com.sniperoftdm.booktracker.ui.library.BookItemUi
import com.sniperoftdm.booktracker.ui.log.QuickLogBookItem
import com.sniperoftdm.booktracker.ui.log.QuickLogContent
import com.sniperoftdm.booktracker.ui.log.QuickLogUiState
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class StageAUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestStats(currentPage: Int = 100, totalPages: Int = 320): BookStats {
        return BookStats(
            currentPage = currentPage,
            pagesLeft = totalPages - currentPage,
            progressPercent = currentPage.toFloat() / totalPages.toFloat(),
            todayPages = 15,
            dailyAverageAllTime = 12.0,
            dailyAverageRecent = 14.0,
            estimatedDaysLeft = 16,
            estimatedFinishDate = LocalDate.now().plusDays(16),
            requiredPagesPerDay = null,
            isOnTrack = null,
            pagesPerDay = emptyMap(),
            sessionsWithPages = emptyList()
        )
    }

    @Test
    fun emptyState_appearsOnFreshDatabase() {
        composeTestRule.setContent {
            BookTrackerTheme {
                HomeScreen(
                    state = HomeUiState(totalBooksCount = 0),
                    onNavigateToDetail = {},
                    onNavigateToAddBook = {},
                    onNavigateToLibrary = {},
                    onNavigateToStats = {},
                    onOpenQuickLog = {},
                    onStopTimer = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Your library is empty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Book").assertIsDisplayed()
    }

    @Test
    fun home_displaysReadingBookCard() {
        val testBook = Book(id = 1L, title = "Atomic Habits", author = "James Clear", totalPages = 320, startPage = 0)
        val testStats = createTestStats(100, 320)

        composeTestRule.setContent {
            BookTrackerTheme {
                HomeScreen(
                    state = HomeUiState(
                        totalBooksCount = 1,
                        readingBooks = listOf(BookItemUi(testBook, testStats))
                    ),
                    onNavigateToDetail = {},
                    onNavigateToAddBook = {},
                    onNavigateToLibrary = {},
                    onNavigateToStats = {},
                    onOpenQuickLog = {},
                    onStopTimer = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Atomic Habits").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reading Now").assertIsDisplayed()
    }

    @Test
    fun quickLog_showsValidationError_forLowerPage() {
        val testBook = Book(id = 1L, title = "Clean Code", totalPages = 400, startPage = 0)

        composeTestRule.setContent {
            BookTrackerTheme {
                QuickLogContent(
                    state = QuickLogUiState(
                        books = listOf(QuickLogBookItem(book = testBook, currentPage = 150)),
                        selectedBookId = 1L,
                        pageInput = "120" // lower than current page 150
                    ),
                    onAction = {}
                )
            }
        }

        composeTestRule.onNodeWithText("End page must be greater than previous checkpoint.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun addBook_validation_disablesSave_whenTitleBlank() {
        composeTestRule.setContent {
            BookTrackerTheme {
                AddEditBookScreen(
                    state = AddEditBookUiState(title = "", totalPagesInput = "300"),
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

        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun clayButton_pressedState_doesNotBreakClicks() {
        var clickCount = 0

        composeTestRule.setContent {
            BookTrackerTheme {
                ClayButton(onClick = { clickCount++ }) {
                    androidx.compose.material3.Text("Squish Click")
                }
            }
        }

        val button = composeTestRule.onNodeWithText("Squish Click")
        button.assertIsDisplayed()
        button.performClick()
        button.performClick()

        assertEquals(2, clickCount)
    }
}
