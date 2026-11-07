package com.sniperoftdm.booktracker.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.data.repository.ActiveTimer
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.BookResult
import com.sniperoftdm.booktracker.data.repository.TimerRepository
import com.sniperoftdm.booktracker.domain.StatsCalculator
import com.sniperoftdm.booktracker.domain.ValidationReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface QuickLogEvent {
    data class Saved(
        val sessionId: Long = 0L,
        val pagesAdded: Int = 0,
        val isFinished: Boolean = false,
        val book: Book? = null
    ) : QuickLogEvent
    data class Error(val reason: ValidationReason) : QuickLogEvent
}

data class QuickLogBookItem(
    val book: Book,
    val currentPage: Int
)

data class QuickLogUiState(
    val books: List<QuickLogBookItem> = emptyList(),
    val selectedBookId: Long? = null,
    val pageInput: String = "",
    val backfillTimestamp: Long? = null,
    val isSaving: Boolean = false,
    val activeTimer: ActiveTimer? = null
)

class QuickLogViewModel(
    private val repository: BookRepository,
    private val timerRepository: TimerRepository? = null,
    private val statsCalculator: StatsCalculator = StatsCalculator()
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickLogUiState())
    val uiState: StateFlow<QuickLogUiState> = _uiState.asStateFlow()

    private val _events = Channel<QuickLogEvent>(Channel.BUFFERED)
    val events: Flow<QuickLogEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeBooks(),
                repository.observeAllSessions()
            ) { bookList, allSessions ->
                val sessionsByBook = allSessions.groupBy { it.bookId }
                bookList.map { book ->
                    val sessions = sessionsByBook[book.id] ?: emptyList()
                    val current = statsCalculator.currentPage(book, sessions)
                    QuickLogBookItem(book = book, currentPage = current)
                }
            }.collect { items ->
                _uiState.update { current ->
                    val selected = current.selectedBookId ?: items.firstOrNull { it.book.status == BookStatus.READING }?.book?.id ?: items.firstOrNull()?.book?.id
                    val defaultPage = if (current.pageInput.isEmpty()) {
                        items.find { it.book.id == selected }?.currentPage?.toString() ?: ""
                    } else current.pageInput
                    current.copy(books = items, selectedBookId = selected, pageInput = defaultPage)
                }
            }
        }

        if (timerRepository != null) {
            viewModelScope.launch {
                timerRepository.observeActive().collect { timer ->
                    _uiState.update { it.copy(activeTimer = timer) }
                }
            }
        }
    }

    fun selectBook(bookId: Long) {
        _uiState.update { current ->
            val item = current.books.find { it.book.id == bookId }
            current.copy(
                selectedBookId = bookId,
                pageInput = item?.currentPage?.toString() ?: current.pageInput
            )
        }
    }

    fun onPageChanged(page: String) {
        _uiState.update { it.copy(pageInput = page) }
    }

    fun stepPage(delta: Int) {
        val current = _uiState.value.pageInput.toIntOrNull() ?: 0
        val next = (current + delta).coerceAtLeast(0)
        _uiState.update { it.copy(pageInput = next.toString()) }
    }

    fun onBackfillTimestampChanged(timestamp: Long?) {
        _uiState.update { it.copy(backfillTimestamp = timestamp) }
    }

    fun startTimer() {
        val bookId = _uiState.value.selectedBookId ?: return
        viewModelScope.launch {
            timerRepository?.start(bookId)
        }
    }

    fun cancelTimer() {
        viewModelScope.launch {
            timerRepository?.cancel()
        }
    }

    fun undoSession(sessionId: Long, bookId: Long) {
        viewModelScope.launch {
            val sessions = repository.observeSessions(bookId).first()
            sessions.find { it.id == sessionId }?.let { session ->
                repository.deleteSession(session)
            }
        }
    }

    fun submit() {
        val currentState = _uiState.value
        val bookId = currentState.selectedBookId ?: return
        val endPage = currentState.pageInput.toIntOrNull()
        if (endPage == null) {
            viewModelScope.launch {
                _events.send(QuickLogEvent.Error(ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS))
            }
            return
        }

        val item = currentState.books.find { it.book.id == bookId }
        val previousPage = item?.currentPage ?: item?.book?.startPage ?: 0
        val pagesAdded = (endPage - previousPage).coerceAtLeast(0)
        val isFinished = item != null && endPage >= item.book.totalPages

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            // If timer is active for this book, use stopAndLog
            val active = currentState.activeTimer
            val result = if (active != null && active.bookId == bookId && currentState.backfillTimestamp == null && timerRepository != null) {
                timerRepository.stopAndLog(endPage)
            } else {
                val timestamp = currentState.backfillTimestamp ?: System.currentTimeMillis()
                repository.logCheckpoint(bookId = bookId, endPage = endPage, timestamp = timestamp)
            }

            when (result) {
                is BookResult.Success -> {
                    _uiState.update { it.copy(pageInput = "", backfillTimestamp = null, isSaving = false) }
                    _events.send(
                        QuickLogEvent.Saved(
                            sessionId = result.data,
                            pagesAdded = pagesAdded,
                            isFinished = isFinished,
                            book = item?.book
                        )
                    )
                }
                is BookResult.ValidationError -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(QuickLogEvent.Error(result.reason))
                }
            }
        }
    }
}
