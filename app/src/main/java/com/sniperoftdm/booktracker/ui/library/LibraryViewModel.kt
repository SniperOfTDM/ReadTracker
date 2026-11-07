package com.sniperoftdm.booktracker.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.BookResult
import com.sniperoftdm.booktracker.data.repository.SettingsRepository
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.domain.BookStats
import com.sniperoftdm.booktracker.domain.StatsCalculator
import com.sniperoftdm.booktracker.domain.StatsConfig
import com.sniperoftdm.booktracker.domain.ValidationReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class BookItemUi(
    val book: Book,
    val stats: BookStats
)

enum class LibrarySortOrder {
    LAST_READ,
    TITLE,
    PROGRESS,
    DATE_ADDED
}

data class LibraryUiState(
    val books: List<BookItemUi> = emptyList(),
    val selectedFilter: BookStatus? = null,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.LAST_READ,
    val countAll: Int = 0,
    val countReading: Int = 0,
    val countPaused: Int = 0,
    val countFinished: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: BookRepository,
    private val statsCalculator: StatsCalculator = StatsCalculator(),
    private val settingsRepository: SettingsRepository? = null,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _filter = MutableStateFlow<BookStatus?>(null)
    private val _sortOrder = MutableStateFlow(LibrarySortOrder.LAST_READ)
    private val _errorEvents = Channel<ValidationReason>(Channel.BUFFERED)
    val errorEvents: Flow<ValidationReason> = _errorEvents.receiveAsFlow()

    private val settingsFlow: Flow<UserSettings> =
        settingsRepository?.settings ?: flowOf(UserSettings())

    val uiState: StateFlow<LibraryUiState> = combine(
        _filter,
        _sortOrder
    ) { filter, sort -> filter to sort }
        .flatMapLatest { (filter, sort) ->
            combine(
                repository.observeBooks(filter),
                repository.observeBooks(null), // For overall counts
                repository.observeAllSessions(),
                settingsFlow
            ) { filteredBooks, allBooks, allSessions, settings ->
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
                val today = LocalDate.now(zone)

                val countAll = allBooks.size
                val countReading = allBooks.count { it.status == BookStatus.READING }
                val countPaused = allBooks.count { it.status == BookStatus.PAUSED }
                val countFinished = allBooks.count { it.status == BookStatus.FINISHED }

                val items = filteredBooks.map { book ->
                    val sessions = sessionsByBook[book.id] ?: emptyList()
                    val stats = activeCalculator.computeBookStats(book, sessions, today, zone)
                    BookItemUi(book = book, stats = stats)
                }

                val sortedItems = when (sort) {
                    LibrarySortOrder.LAST_READ -> items.sortedByDescending { item ->
                        val sessions = sessionsByBook[item.book.id] ?: emptyList()
                        sessions.maxByOrNull { it.timestamp }?.timestamp ?: item.book.createdAt
                    }
                    LibrarySortOrder.TITLE -> items.sortedBy { it.book.title.lowercase() }
                    LibrarySortOrder.PROGRESS -> items.sortedByDescending { it.stats.progressPercent }
                    LibrarySortOrder.DATE_ADDED -> items.sortedByDescending { it.book.createdAt }
                }

                LibraryUiState(
                    books = sortedItems,
                    selectedFilter = filter,
                    sortOrder = sort,
                    countAll = countAll,
                    countReading = countReading,
                    countPaused = countPaused,
                    countFinished = countFinished,
                    isLoading = false
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState()
        )

    fun setFilter(status: BookStatus?) {
        _filter.value = status
    }

    fun setSortOrder(sortOrder: LibrarySortOrder) {
        _sortOrder.value = sortOrder
    }

    fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        startPage: Int,
        goalDateEpochDay: Long? = null
    ) {
        viewModelScope.launch {
            when (val result = repository.addBook(title, author, totalPages, startPage, goalDateEpochDay)) {
                is BookResult.Success -> Unit
                is BookResult.ValidationError -> _errorEvents.send(result.reason)
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }

    fun setStatus(bookId: Long, status: BookStatus) {
        viewModelScope.launch {
            repository.setStatus(bookId, status)
        }
    }

    fun markFinished(book: Book) {
        viewModelScope.launch {
            repository.logCheckpoint(bookId = book.id, endPage = book.totalPages)
        }
    }
}
