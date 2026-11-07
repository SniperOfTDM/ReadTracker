package com.sniperoftdm.booktracker.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.BookResult
import com.sniperoftdm.booktracker.domain.SessionValidator
import com.sniperoftdm.booktracker.domain.StatsCalculator
import com.sniperoftdm.booktracker.domain.ValidationReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class AddEditBookUiState(
    val bookId: Long? = null,
    val title: String = "",
    val author: String = "",
    val totalPagesInput: String = "",
    val startPageInput: String = "0",
    val currentPage: Int = 0,
    val goalDate: LocalDate? = null,
    val status: BookStatus = BookStatus.READING,
    val initialBook: Book? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val requiredPaceLive: Int? = null
) {
    val isEditing: Boolean get() = bookId != null
    val hasUnsavedChanges: Boolean
        get() {
            if (initialBook == null) {
                return title.isNotBlank() || author.isNotBlank() || totalPagesInput.isNotBlank() || startPageInput != "0" || goalDate != null
            }
            return title != initialBook.title ||
                    author != initialBook.author ||
                    totalPagesInput != initialBook.totalPages.toString() ||
                    startPageInput != initialBook.startPage.toString() ||
                    status != initialBook.status ||
                    (goalDate?.toEpochDay() != initialBook.goalDateEpochDay)
        }
}

sealed interface AddEditBookEvent {
    data object Saved : AddEditBookEvent
    data class Error(val reason: ValidationReason) : AddEditBookEvent
}

class AddEditBookViewModel(
    private val repository: BookRepository,
    private val statsCalculator: StatsCalculator = StatsCalculator(),
    private val validator: SessionValidator = SessionValidator(),
    private val bookId: Long? = null,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditBookUiState(bookId = bookId, isLoading = bookId != null))
    val uiState: StateFlow<AddEditBookUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditBookEvent>(Channel.BUFFERED)
    val events: Flow<AddEditBookEvent> = _events.receiveAsFlow()

    init {
        if (bookId != null && bookId > 0) {
            viewModelScope.launch {
                val book = repository.observeBook(bookId).first()
                if (book != null) {
                    val sessions = repository.observeSessions(bookId).first()
                    val currentPage = statsCalculator.currentPage(book, sessions)
                    _uiState.update { current ->
                        current.copy(
                            title = book.title,
                            author = book.author,
                            totalPagesInput = book.totalPages.toString(),
                            startPageInput = book.startPage.toString(),
                            currentPage = currentPage,
                            goalDate = book.goalDateEpochDay?.let { LocalDate.ofEpochDay(it) },
                            status = book.status,
                            initialBook = book,
                            isLoading = false
                        )
                    }
                    recalculateRequiredPace()
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onAuthorChange(value: String) {
        _uiState.update { it.copy(author = value) }
    }

    fun onTotalPagesChange(value: String) {
        _uiState.update { it.copy(totalPagesInput = value) }
        recalculateRequiredPace()
    }

    fun onStartPageChange(value: String) {
        _uiState.update { it.copy(startPageInput = value) }
        recalculateRequiredPace()
    }

    fun onGoalDateChange(date: LocalDate?) {
        _uiState.update { it.copy(goalDate = date) }
        recalculateRequiredPace()
    }

    fun onStatusChange(newStatus: BookStatus) {
        _uiState.update { it.copy(status = newStatus) }
    }

    private fun recalculateRequiredPace() {
        val state = _uiState.value
        val total = state.totalPagesInput.toIntOrNull() ?: return
        val start = state.startPageInput.toIntOrNull() ?: 0
        val goal = state.goalDate ?: run {
            _uiState.update { it.copy(requiredPaceLive = null) }
            return
        }

        val today = LocalDate.now(zone)
        val tempBook = Book(
            id = state.bookId ?: 0L,
            title = state.title,
            author = state.author,
            totalPages = total,
            startPage = start,
            status = state.status,
            goalDateEpochDay = goal.toEpochDay()
        )

        val pace = statsCalculator.requiredPagesPerDay(tempBook, emptyList(), today)
        _uiState.update { it.copy(requiredPaceLive = pace) }
    }

    fun validate(): ValidationReason? {
        val state = _uiState.value
        val total = state.totalPagesInput.toIntOrNull()
        val start = state.startPageInput.toIntOrNull()

        if (state.title.isBlank()) return ValidationReason.TITLE_BLANK
        if (total == null || total <= 0) return ValidationReason.TOTAL_PAGES_NOT_POSITIVE
        if (start == null || start < 0 || start > total) return ValidationReason.START_PAGE_OUT_OF_RANGE

        if (state.isEditing && total < state.currentPage) {
            return ValidationReason.TOTAL_PAGES_BELOW_CURRENT
        }

        return null
    }

    fun save() {
        val validation = validate()
        if (validation != null) {
            viewModelScope.launch {
                _events.send(AddEditBookEvent.Error(validation))
            }
            return
        }

        val state = _uiState.value
        val total = state.totalPagesInput.toIntOrNull() ?: return
        val start = state.startPageInput.toIntOrNull() ?: 0
        val goalEpochDay = state.goalDate?.toEpochDay()

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val result = if (state.isEditing && state.initialBook != null) {
                val updatedBook = state.initialBook.copy(
                    title = state.title.trim(),
                    author = state.author.trim(),
                    totalPages = total,
                    startPage = start,
                    status = state.status,
                    goalDateEpochDay = goalEpochDay
                )
                repository.updateBook(updatedBook)
            } else {
                repository.addBook(
                    title = state.title.trim(),
                    author = state.author.trim(),
                    totalPages = total,
                    startPage = start,
                    goalDateEpochDay = goalEpochDay
                )
            }

            when (result) {
                is BookResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AddEditBookEvent.Saved)
                }
                is BookResult.ValidationError -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AddEditBookEvent.Error(result.reason))
                }
            }
        }
    }
}
