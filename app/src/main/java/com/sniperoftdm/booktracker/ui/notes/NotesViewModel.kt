package com.sniperoftdm.booktracker.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperoftdm.booktracker.data.model.Note
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.BookResult
import com.sniperoftdm.booktracker.domain.ValidationReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NotesEvent {
    data object Saved : NotesEvent
    data class Error(val reason: ValidationReason) : NotesEvent
}

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true
)

class NotesViewModel(
    private val bookId: Long,
    private val repository: BookRepository
) : ViewModel() {

    private val _events = Channel<NotesEvent>(Channel.BUFFERED)
    val events: Flow<NotesEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<NotesUiState> = repository.observeNotes(bookId)
        .map { NotesUiState(notes = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesUiState()
        )

    fun addNote(page: Int, text: String) {
        viewModelScope.launch {
            when (val result = repository.addNote(bookId = bookId, page = page, text = text)) {
                is BookResult.Success -> _events.send(NotesEvent.Saved)
                is BookResult.ValidationError -> _events.send(NotesEvent.Error(result.reason))
            }
        }
    }

    fun editNote(note: Note) {
        viewModelScope.launch {
            when (val result = repository.editNote(note)) {
                is BookResult.Success -> _events.send(NotesEvent.Saved)
                is BookResult.ValidationError -> _events.send(NotesEvent.Error(result.reason))
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
}
