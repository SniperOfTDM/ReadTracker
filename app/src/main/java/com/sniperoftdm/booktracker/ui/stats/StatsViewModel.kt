package com.sniperoftdm.booktracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.SettingsRepository
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.domain.GlobalStats
import com.sniperoftdm.booktracker.domain.StatsCalculator
import com.sniperoftdm.booktracker.domain.StatsConfig
import com.sniperoftdm.booktracker.domain.TotalEta
import com.sniperoftdm.booktracker.domain.YearlyGoalProgress
import com.sniperoftdm.booktracker.ui.components.BarChartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class StatsPeriod {
    WEEK,
    MONTH,
    ALL_TIME
}

data class DayBookSessions(
    val book: Book,
    val totalPages: Int,
    val sessions: List<Pair<Session, Int>>
)

data class StatsUiState(
    val globalStats: GlobalStats = GlobalStats(
        currentStreak = 0,
        longestStreak = 0,
        bestDay = null,
        totalPagesRead = 0,
        pagesPerDay = emptyMap()
    ),
    val selectedPeriod: StatsPeriod = StatsPeriod.WEEK,
    val chartItems: List<BarChartItem> = emptyList(),
    val periodAverage: Double = 0.0,
    val yearlyGoalProgress: YearlyGoalProgress? = null,
    val projectedBooksByYearEnd: Int = 0,
    val totalEta: TotalEta? = null,
    val booksFinishedThisYear: Int = 0,
    val selectedDay: LocalDate? = null,
    val selectedDaySessions: List<DayBookSessions> = emptyList(),
    val isLoading: Boolean = true
)

class StatsViewModel(
    private val repository: BookRepository,
    private val statsCalculator: StatsCalculator = StatsCalculator(),
    private val settingsRepository: SettingsRepository? = null,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.WEEK)
    private val _selectedDay = MutableStateFlow<LocalDate?>(null)

    private val settingsFlow: Flow<UserSettings> =
        settingsRepository?.settings ?: flowOf(UserSettings())

    val uiState: StateFlow<StatsUiState> = combine(
        repository.observeBooks(),
        repository.observeAllSessions(),
        settingsFlow,
        _selectedPeriod,
        _selectedDay
    ) { books, allSessions, settings, period, selectedDay ->
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

        val sessionsByBook = allSessions.groupBy { it.bookId }
        val booksById = books.associateBy { it.id }
        val today = LocalDate.now(zone)
        val globalStats = activeCalculator.computeGlobalStats(books, sessionsByBook, today, zone)

        // Build bar chart based on selected period
        val daysInPeriod = when (period) {
            StatsPeriod.WEEK -> 7
            StatsPeriod.MONTH -> 30
            StatsPeriod.ALL_TIME -> 60
        }

        val chartItems = (daysInPeriod - 1 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val pages = globalStats.pagesPerDay[date] ?: 0
            val label = when (period) {
                StatsPeriod.WEEK -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)
                StatsPeriod.MONTH -> date.format(DateTimeFormatter.ofPattern("d"))
                StatsPeriod.ALL_TIME -> date.format(DateTimeFormatter.ofPattern("d/M"))
            }
            BarChartItem(date = date, pages = pages, label = label)
        }

        val totalPagesInPeriod = chartItems.sumOf { it.pages }
        val periodAvg = if (daysInPeriod > 0) totalPagesInPeriod.toDouble() / daysInPeriod.toDouble() else 0.0

        val readingBooks = books.filter { it.status == BookStatus.READING }
        val yearlyGoal = settings.yearlyGoalBooks
        val yearlyProgress = if (yearlyGoal > 0) {
            activeCalculator.yearlyGoalProgress(
                year = today.year,
                goal = yearlyGoal,
                today = today,
                books = books,
                sessionsByBook = sessionsByBook,
                zone = zone
            )
        } else {
            null
        }

        val projected = activeCalculator.projectedBooksByYearEnd(
            year = today.year,
            today = today,
            books = books,
            sessionsByBook = sessionsByBook,
            zone = zone
        )

        val totalEta = activeCalculator.totalEta(
            readingBooks = readingBooks,
            sessionsByBook = sessionsByBook,
            today = today,
            zone = zone
        )

        val finishedThisYear = activeCalculator.booksFinishedInYear(
            year = today.year,
            books = books,
            sessionsByBook = sessionsByBook,
            zone = zone
        ).size

        // Heatmap Day Selection Drilldown
        val daySessions = if (selectedDay != null) {
            val sessionsOnDay = allSessions.filter { session ->
                Instant.ofEpochMilli(session.timestamp).atZone(zone).toLocalDate() == selectedDay
            }

            sessionsOnDay.groupBy { it.bookId }.mapNotNull { (bookId, sessions) ->
                val book = booksById[bookId] ?: return@mapNotNull null
                val bookSessions = sessionsByBook[bookId] ?: emptyList()
                val sessionPairs = activeCalculator.pagesPerSession(book, bookSessions)
                    .filter { pair -> sessions.any { it.id == pair.first.id } }
                val totalPages = sessionPairs.sumOf { it.second }
                DayBookSessions(
                    book = book,
                    totalPages = totalPages,
                    sessions = sessionPairs
                )
            }
        } else emptyList()

        StatsUiState(
            globalStats = globalStats,
            selectedPeriod = period,
            chartItems = chartItems,
            periodAverage = periodAvg,
            yearlyGoalProgress = yearlyProgress,
            projectedBooksByYearEnd = projected,
            totalEta = totalEta,
            booksFinishedThisYear = finishedThisYear,
            selectedDay = selectedDay,
            selectedDaySessions = daySessions,
            isLoading = false
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState()
        )

    fun selectPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
    }

    fun selectDay(date: LocalDate) {
        _selectedDay.value = date
    }

    fun clearSelectedDay() {
        _selectedDay.value = null
    }

    fun setYearlyGoal(goal: Int) {
        viewModelScope.launch {
            settingsRepository?.setYearlyGoalBooks(goal)
        }
    }
}
