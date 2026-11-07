package com.sniperoftdm.booktracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperoftdm.booktracker.data.remote.IsbnValidator
import com.sniperoftdm.booktracker.data.repository.BookLookupRepository
import com.sniperoftdm.booktracker.domain.BookCandidate
import com.sniperoftdm.booktracker.domain.LookupResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Categorized errors displayed on the book search screen.
 */
sealed interface SearchError {
    data object Offline : SearchError
    data object RateLimited : SearchError
    data object InvalidIsbn : SearchError
    data class HttpError(val code: Int) : SearchError
    data object GenericError : SearchError
}

/**
 * UI state for online book search screen.
 */
data class SearchBooksUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val candidates: List<BookCandidate> = emptyList(),
    val error: SearchError? = null,
    val hasSearched: Boolean = false,
    val isScannerAvailable: Boolean = false
)

/**
 * ViewModel managing debounced book queries, ISBN lookup, and error handling.
 */
class SearchBooksViewModel(
    private val repository: BookLookupRepository,
    isScannerAvailable: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchBooksUiState(isScannerAvailable = isScannerAvailable))
    val uiState: StateFlow<SearchBooksUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /** Update query input with 700ms debounce when text is at least 3 characters. */
    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, error = null) }
        searchJob?.cancel()

        val trimmed = newQuery.trim()
        if (trimmed.length < 3) {
            _uiState.update { it.copy(isLoading = false, candidates = emptyList(), hasSearched = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(700)
            performSearch(trimmed)
        }
    }

    /** Trigger search immediately on keyboard IME action. */
    fun onSearchAction() {
        searchJob?.cancel()
        val trimmed = _uiState.value.query.trim()
        if (trimmed.isNotBlank()) {
            searchJob = viewModelScope.launch {
                performSearch(trimmed)
            }
        }
    }

    /** Route scanned ISBN directly to ISBN lookup. */
    fun onIsbnScanned(isbn: String) {
        _uiState.update { it.copy(query = isbn, error = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performIsbnLookup(isbn)
        }
    }

    /** Retry current search after network error. */
    fun retry() {
        onSearchAction()
    }

    private suspend fun performSearch(query: String) {
        val clean = IsbnValidator.clean(query)
        if ((clean.length == 10 || clean.length == 13) && IsbnValidator.isValid(clean)) {
            performIsbnLookup(clean)
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, hasSearched = true) }
        val result = repository.search(query, page = 1)
        handleResult(result)
    }

    private suspend fun performIsbnLookup(isbn: String) {
        _uiState.update { it.copy(isLoading = true, error = null, hasSearched = true) }
        val result = repository.lookupIsbn(isbn)
        handleResult(result)
    }

    private fun handleResult(result: LookupResult) {
        when (result) {
            is LookupResult.Success -> {
                _uiState.update { it.copy(isLoading = false, candidates = result.candidates, error = null) }
            }
            is LookupResult.Offline -> {
                _uiState.update { it.copy(isLoading = false, error = SearchError.Offline) }
            }
            is LookupResult.RateLimited -> {
                _uiState.update { it.copy(isLoading = false, error = SearchError.RateLimited) }
            }
            is LookupResult.InvalidIsbn -> {
                _uiState.update { it.copy(isLoading = false, error = SearchError.InvalidIsbn) }
            }
            is LookupResult.HttpError -> {
                _uiState.update { it.copy(isLoading = false, error = SearchError.HttpError(result.code)) }
            }
            is LookupResult.Timeout, is LookupResult.ParseError, LookupResult.Unavailable -> {
                _uiState.update { it.copy(isLoading = false, error = SearchError.GenericError) }
            }
        }
    }
}
