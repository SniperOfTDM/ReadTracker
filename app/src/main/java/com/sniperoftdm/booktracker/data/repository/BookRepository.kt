package com.sniperoftdm.booktracker.data.repository

import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Note
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.domain.ValidationReason
import kotlinx.coroutines.flow.Flow

/** Outcome of a write operation that may fail with a validation error. */
sealed interface BookResult<out T> {
    data class Success<T>(val data: T) : BookResult<T>
    data class ValidationError(val reason: ValidationReason) : BookResult<Nothing>
}

interface BookRepository {

    /** Insert a new book after validation. Returns the generated id on success. */
    suspend fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        startPage: Int,
        goalDateEpochDay: Long? = null
    ): BookResult<Long>

    /** Update an existing book after validation. */
    suspend fun updateBook(book: Book): BookResult<Unit>

    /** Delete a book and its cascade sessions. */
    suspend fun deleteBook(book: Book)

    /** Set the status of a book by id. */
    suspend fun setStatus(bookId: Long, status: BookStatus)

    /** Record a reading checkpoint for a book. */
    suspend fun logCheckpoint(
        bookId: Long,
        endPage: Int,
        timestamp: Long = System.currentTimeMillis(),
        durationSeconds: Int? = null
    ): BookResult<Long>

    /** Edit an existing session after validation. */
    suspend fun editSession(session: Session): BookResult<Unit>

    /** Delete a session and reconcile the book's status. */
    suspend fun deleteSession(session: Session)

    /** Observe books, optionally filtered by status. */
    fun observeBooks(status: BookStatus? = null): Flow<List<Book>>

    /** Observe a single book by id. */
    fun observeBook(id: Long): Flow<Book?>

    /** Observe all sessions for a book, ordered chronologically. */
    fun observeSessions(bookId: Long): Flow<List<Session>>

    /** Observe every session across all books. */
    fun observeAllSessions(): Flow<List<Session>>

    /** Record a note attached to a page in a book. */
    suspend fun addNote(bookId: Long, page: Int, text: String): BookResult<Long>

    /** Edit an existing note after validation. */
    suspend fun editNote(note: Note): BookResult<Unit>

    /** Delete a note. */
    suspend fun deleteNote(note: Note)

    /** Observe notes for a book, ordered by page then creation time. */
    fun observeNotes(bookId: Long): Flow<List<Note>>
}
