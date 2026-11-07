package com.sniperoftdm.booktracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.repository.ActiveTimer
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.SettingsRepository
import com.sniperoftdm.booktracker.data.repository.TimerRepository
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.domain.BookStats
import com.sniperoftdm.booktracker.domain.StatsCalculator
import com.sniperoftdm.booktracker.domain.StatsConfig
import com.sniperoftdm.booktracker.domain.TotalEta
import com.sniperoftdm.booktracker.domain.YearlyGoalProgress
import com.sniperoftdm.booktracker.ui.library.BookItemUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val todayPages: Int = 0,
    val booksTouchedToday: Int = 0,
    val currentStreak: Int = 0,
    val encouragementText: String = "",
    val activeTimer: ActiveTimer? = null,
    val activeTimerBookTitle: String? = null,
    val readingBooks: List<BookItemUi> = emptyList(),
    val totalBooksCount: Int = 0,
    val totalEta: TotalEta? = null,
    val yearlyGoalProgress: YearlyGoalProgress? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: BookRepository,
    private val timerRepository: TimerRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val statsCalculator: StatsCalculator = StatsCalculator(),
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val settingsFlow: Flow<UserSettings> =
        settingsRepository?.settings ?: flowOf(UserSettings())

    private val timerFlow: Flow<ActiveTimer?> =
        timerRepository?.observeActive() ?: flowOf(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeBooks(),
        repository.observeAllSessions(),
        timerFlow,
        settingsFlow
    ) { books, allSessions, activeTimer, settings ->
        val activeCalculator = if (settingsRepository != null) {
            StatsCalculator(
                StatsConfig(
                    countZeroDays = settings.countZeroDays,
                    paceWindowDays = settings.paceWindowDays
                )
            )
        } else {
            statsCalculator
        }

        val today = LocalDate.now(zone)
        val sessionsByBook = allSessions.groupBy { it.bookId }

        // Today's pages and books touched
        val todaySessions = allSessions.filter { session ->
            Instant.ofEpochMilli(session.timestamp).atZone(zone).toLocalDate() == today
        }
        val todayPages = books.sumOf { book ->
            val bookSessions = sessionsByBook[book.id] ?: emptyList()
            activeCalculator.todayPages(book, bookSessions, today, zone)
        }
        val booksTouchedToday = todaySessions.map { it.bookId }.distinct().size

        // Streak
        val globalDaily = activeCalculator.globalPagesPerDay(books, sessionsByBook, zone)
        val currentStreak = activeCalculator.currentStreak(globalDaily, today)

        // Encouragement text
        val encouragement = when {
            todayPages > 0 -> "$todayPages pages today"
            currentStreak > 0 -> "Streak alive: log today to keep it"
            else -> "Nothing logged yet today"
        }

        // Active timer book title
        val timerBookTitle = activeTimer?.let { timer ->
            books.find { it.id == timer.bookId }?.title
        }

        // Reading books ordered by most recently logged session
        val readingBooksList = books
            .filter { it.status == BookStatus.READING }
            .map { book ->
                val sessions = sessionsByBook[book.id] ?: emptyList()
                val stats = activeCalculator.computeBookStats(book, sessions, today, zone)
                val lastTimestamp = sessions.maxByOrNull { it.timestamp }?.timestamp ?: book.createdAt
                Triple(book, stats, lastTimestamp)
            }
            .sortedByDescending { it.third }
            .map { BookItemUi(book = it.first, stats = it.second) }

        // Total ETA when 2+ books reading
        val readingOnlyBooks = books.filter { it.status == BookStatus.READING }
        val totalEta = if (readingOnlyBooks.size >= 2) {
            activeCalculator.totalEta(readingOnlyBooks, sessionsByBook, today, zone)
        } else null

        // Yearly Goal
        val yearlyGoalProgress = if (settings.yearlyGoalBooks > 0) {
            activeCalculator.yearlyGoalProgress(
                year = today.year,
                goal = settings.yearlyGoalBooks,
                today = today,
                books = books,
                sessionsByBook = sessionsByBook,
                zone = zone
            )
        } else null

        HomeUiState(
            todayPages = todayPages,
            booksTouchedToday = booksTouchedToday,
            currentStreak = currentStreak,
            encouragementText = encouragement,
            activeTimer = activeTimer,
            activeTimerBookTitle = timerBookTitle,
            readingBooks = readingBooksList,
            totalBooksCount = books.size,
            totalEta = totalEta,
            yearlyGoalProgress = yearlyGoalProgress,
            isLoading = false
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun cancelTimer() {
        viewModelScope.launch {
            timerRepository?.cancel()
        }
    }
}
