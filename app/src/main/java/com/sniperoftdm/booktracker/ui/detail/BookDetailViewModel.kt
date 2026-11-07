package com.sniperoftdm.booktracker.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Note
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.data.repository.ActiveTimer
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.BookResult
import com.sniperoftdm.booktracker.data.repository.SettingsRepository
import com.sniperoftdm.booktracker.data.repository.TimerRepository
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.domain.BookStats
import com.sniperoftdm.booktracker.domain.StatsCalculator
import com.sniperoftdm.booktracker.domain.StatsConfig
import com.sniperoftdm.booktracker.domain.ValidationReason
import com.sniperoftdm.booktracker.ui.components.BarChartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

sealed interface DayListItem {
    data class ReadingDay(
        val dayNumber: Long,
        val date: LocalDate,
        val totalPagesRead: Int,
        val sessions: List<Pair<Session, Int>>
    ) : DayListItem {
        val dayLabel: String get() = "Day $dayNumber"
    }

    data class ZeroReadingGap(
        val daysCount: Int,
        val startDate: LocalDate,
        val endDate: LocalDate
    ) : DayListItem
}

object BookDetailHelper {
    fun buildDayList(
        sessionsWithPages: List<Pair<Session, Int>>,
        zone: ZoneId
    ): List<DayListItem> {
        if (sessionsWithPages.isEmpty()) return emptyList()

        val grouped = sessionsWithPages.groupBy { (session, _) ->
            Instant.ofEpochMilli(session.timestamp).atZone(zone).toLocalDate()
        }

        val sortedDates = grouped.keys.sorted()
        val firstDate = sortedDates.first()

        val resultAsc = mutableListOf<DayListItem>()
        var previousDate: LocalDate? = null

        for (date in sortedDates) {
            if (previousDate != null) {
                val gap = ChronoUnit.DAYS.between(previousDate, date) - 1
                if (gap > 0) {
                    resultAsc.add(
                        DayListItem.ZeroReadingGap(
                            daysCount = gap.toInt(),
                            startDate = previousDate.plusDays(1),
                            endDate = date.minusDays(1)
                        )
                    )
                }
            }

            val dayNumber = ChronoUnit.DAYS.between(firstDate, date) + 1
            val daySessions = grouped[date] ?: emptyList()
            val dayPages = daySessions.sumOf { it.second }

            resultAsc.add(
                DayListItem.ReadingDay(
                    dayNumber = dayNumber,
                    date = date,
                    totalPagesRead = dayPages,
                    sessions = daySessions.sortedByDescending { it.first.timestamp }
                )
            )
            previousDate = date
        }

        return resultAsc.reversed()
    }
}

data class BookDetailUiState(
    val book: Book? = null,
    val stats: BookStats? = null,
    val dayListItems: List<DayListItem> = emptyList(),
    val notes: List<Note> = emptyList(),
    val last14Days: List<BarChartItem> = emptyList(),
    val isTimerActive: Boolean = false,
    val activeTimerStartedAtMillis: Long? = null,
    val daysReadingCount: Long = 0L,
    val isLoading: Boolean = true
) {
    val dayGroups: List<DayListItem.ReadingDay> get() = dayListItems.filterIsInstance<DayListItem.ReadingDay>()
}

class BookDetailViewModel(
    val bookId: Long,
    private val repository: BookRepository,
    private val statsCalculator: StatsCalculator = StatsCalculator(),
    private val settingsRepository: SettingsRepository? = null,
    private val timerRepository: TimerRepository? = null,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _errorEvents = Channel<ValidationReason>(Channel.BUFFERED)
    val errorEvents: Flow<ValidationReason> = _errorEvents.receiveAsFlow()

    private var lastDeletedSession: Session? = null
    private var lastDeletedNote: Note? = null

    private val settingsFlow: Flow<UserSettings> =
        settingsRepository?.settings ?: flowOf(UserSettings())

    private val timerFlow: Flow<ActiveTimer?> =
        timerRepository?.observeActive() ?: flowOf(null)

    val uiState: StateFlow<BookDetailUiState> = combine(
        repository.observeBook(bookId),
        repository.observeSessions(bookId),
        repository.observeNotes(bookId),
        settingsFlow,
        timerFlow
    ) { book, sessions, notes, settings, activeTimer ->
        if (book == null) {
            BookDetailUiState(isLoading = false)
        } else {
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
            val stats = activeCalculator.computeBookStats(book, sessions, today, zone)
            val dayListItems = BookDetailHelper.buildDayList(stats.sessionsWithPages, zone)

            // Last 14 days chart
            val last14 = (13 downTo 0).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val pages = stats.pagesPerDay[date] ?: 0
                val label = date.format(DateTimeFormatter.ofPattern("d"))
                BarChartItem(date = date, pages = pages, label = label)
            }

            // Total reading days count
            val daysReading = if (sessions.isEmpty()) {
                0L
            } else {
                val firstSessionTime = sessions.minOfOrNull { it.timestamp } ?: book.createdAt
                val firstDate = Instant.ofEpochMilli(firstSessionTime).atZone(zone).toLocalDate()
                ChronoUnit.DAYS.between(firstDate, today) + 1
            }

            val isTimerRunning = activeTimer != null && activeTimer.bookId == bookId

            // Sorted notes: page ascending, then date descending
            val sortedNotes = notes.sortedWith(
                compareBy<Note> { it.page }.thenByDescending { it.createdAt }
            )

            BookDetailUiState(
                book = book,
                stats = stats,
                dayListItems = dayListItems,
                notes = sortedNotes,
                last14Days = last14,
                isTimerActive = isTimerRunning,
                activeTimerStartedAtMillis = if (isTimerRunning) activeTimer?.startedAtMillis else null,
                daysReadingCount = maxOf(1L, daysReading),
                isLoading = false
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookDetailUiState()
        )

    fun editSession(session: Session) {
        viewModelScope.launch {
            when (val result = repository.editSession(session)) {
                is BookResult.Success -> Unit
                is BookResult.ValidationError -> _errorEvents.send(result.reason)
            }
        }
    }

    fun deleteSession(session: Session) {
        lastDeletedSession = session
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun undoDeleteSession() {
        val session = lastDeletedSession ?: return
        lastDeletedSession = null
        viewModelScope.launch {
            repository.logCheckpoint(
                bookId = session.bookId,
                endPage = session.endPage,
                timestamp = session.timestamp,
                durationSeconds = session.durationSeconds
            )
        }
    }

    fun addNote(page: Int, text: String) {
        viewModelScope.launch {
            when (val result = repository.addNote(bookId = bookId, page = page, text = text)) {
                is BookResult.Success -> Unit
                is BookResult.ValidationError -> _errorEvents.send(result.reason)
            }
        }
    }

    fun editNote(note: Note) {
        viewModelScope.launch {
            when (val result = repository.editNote(note)) {
                is BookResult.Success -> Unit
                is BookResult.ValidationError -> _errorEvents.send(result.reason)
            }
        }
    }

    fun deleteNote(note: Note) {
        lastDeletedNote = note
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun undoDeleteNote() {
        val note = lastDeletedNote ?: return
        lastDeletedNote = null
        viewModelScope.launch {
            repository.addNote(
                bookId = note.bookId,
                page = note.page,
                text = note.text
            )
        }
    }

    fun setStatus(status: BookStatus) {
        viewModelScope.launch {
            repository.setStatus(bookId, status)
        }
    }

    fun markFinished() {
        val currentBook = uiState.value.book ?: return
        viewModelScope.launch {
            repository.setStatus(bookId, BookStatus.FINISHED)
            repository.logCheckpoint(
                bookId = bookId,
                endPage = currentBook.totalPages,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun deleteBook() {
        val currentBook = uiState.value.book ?: return
        viewModelScope.launch {
            repository.deleteBook(currentBook)
        }
    }

    fun startTimer() {
        viewModelScope.launch {
            timerRepository?.start(bookId)
        }
    }

    fun cancelTimer() {
        viewModelScope.launch {
            timerRepository?.cancel()
        }
    }
}
