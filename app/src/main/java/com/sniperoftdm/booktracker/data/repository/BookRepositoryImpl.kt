package com.sniperoftdm.booktracker.data.repository

import com.sniperoftdm.booktracker.data.local.BookDao
import com.sniperoftdm.booktracker.data.local.NoteDao
import com.sniperoftdm.booktracker.data.local.SessionDao
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Note
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.domain.SessionValidator
import com.sniperoftdm.booktracker.domain.ValidationReason
import com.sniperoftdm.booktracker.domain.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BookRepositoryImpl(
    private val bookDao: BookDao,
    private val sessionDao: SessionDao,
    private val noteDao: NoteDao? = null,
    private val validator: SessionValidator = SessionValidator(),
    private val onDataChanged: (() -> Unit)? = null
) : BookRepository {

    override suspend fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        startPage: Int,
        goalDateEpochDay: Long?
    ): BookResult<Long> = withContext(Dispatchers.IO) {
        val trimmedTitle = title.trim()
        val trimmedAuthor = author.trim()

        val validation = validator.validateBook(
            title = trimmedTitle,
            totalPages = totalPages,
            startPage = startPage
        )
        if (validation is ValidationResult.Invalid) {
            return@withContext BookResult.ValidationError(validation.reason)
        }

        val book = Book(
            title = trimmedTitle,
            author = trimmedAuthor,
            totalPages = totalPages,
            startPage = startPage,
            status = BookStatus.READING,
            goalDateEpochDay = goalDateEpochDay,
            createdAt = System.currentTimeMillis()
        )
        val id = bookDao.insert(book)
        onDataChanged?.invoke()
        BookResult.Success(id)
    }

    override suspend fun updateBook(book: Book): BookResult<Unit> = withContext(Dispatchers.IO) {
        val lastSession = sessionDao.getLastForBook(book.id)
        val currentPage = lastSession?.endPage ?: book.startPage

        val validation = validator.validateBookEdit(book, currentPage)
        if (validation is ValidationResult.Invalid) {
            return@withContext BookResult.ValidationError(validation.reason)
        }

        bookDao.update(book)
        reconcileBookStatus(book.id)
        onDataChanged?.invoke()
        BookResult.Success(Unit)
    }

    override suspend fun deleteBook(book: Book): Unit = withContext(Dispatchers.IO) {
        bookDao.delete(book)
        onDataChanged?.invoke()
    }

    override suspend fun setStatus(bookId: Long, status: BookStatus): Unit = withContext(Dispatchers.IO) {
        val book = bookDao.getById(bookId) ?: return@withContext
        bookDao.update(book.copy(status = status))
        onDataChanged?.invoke()
    }

    override suspend fun logCheckpoint(
        bookId: Long,
        endPage: Int,
        timestamp: Long,
        durationSeconds: Int?
    ): BookResult<Long> = withContext(Dispatchers.IO) {
        val book = bookDao.getById(bookId)
            ?: return@withContext BookResult.ValidationError(ValidationReason.TITLE_BLANK)

        val previousSession = sessionDao.getPreviousSession(bookId, timestamp, 0L)
        val nextSession = sessionDao.getNextSession(bookId, timestamp, 0L)

        val validation = validator.validateNewSession(
            endPage = endPage,
            timestamp = timestamp,
            book = book,
            previousSession = previousSession,
            nextSession = nextSession
        )
        if (validation is ValidationResult.Invalid) {
            return@withContext BookResult.ValidationError(validation.reason)
        }

        val session = Session(
            bookId = bookId,
            timestamp = timestamp,
            endPage = endPage,
            durationSeconds = durationSeconds
        )
        val sessionId = sessionDao.insert(session)
        reconcileBookStatus(bookId)
        onDataChanged?.invoke()
        BookResult.Success(sessionId)
    }

    override suspend fun editSession(session: Session): BookResult<Unit> = withContext(Dispatchers.IO) {
        val book = bookDao.getById(session.bookId)
            ?: return@withContext BookResult.ValidationError(ValidationReason.TITLE_BLANK)

        val previousSession = sessionDao.getPreviousSession(session.bookId, session.timestamp, session.id)
        val nextSession = sessionDao.getNextSession(session.bookId, session.timestamp, session.id)

        val validation = validator.validateEditSession(
            session = session,
            book = book,
            previousSession = previousSession,
            nextSession = nextSession
        )
        if (validation is ValidationResult.Invalid) {
            return@withContext BookResult.ValidationError(validation.reason)
        }

        sessionDao.update(session)
        reconcileBookStatus(session.bookId)
        onDataChanged?.invoke()
        BookResult.Success(Unit)
    }

    override suspend fun deleteSession(session: Session): Unit = withContext(Dispatchers.IO) {
        sessionDao.delete(session)
        reconcileBookStatus(session.bookId)
        onDataChanged?.invoke()
    }

    override fun observeBooks(status: BookStatus?): Flow<List<Book>> {
        return if (status == null) {
            bookDao.observeAll()
        } else {
            bookDao.observeByStatus(status)
        }
    }

    override fun observeBook(id: Long): Flow<Book?> {
        return bookDao.observeById(id)
    }

    override fun observeSessions(bookId: Long): Flow<List<Session>> {
        return sessionDao.observeForBook(bookId)
    }

    override fun observeAllSessions(): Flow<List<Session>> {
        return sessionDao.observeAll()
    }

    override suspend fun addNote(bookId: Long, page: Int, text: String): BookResult<Long> = withContext(Dispatchers.IO) {
        val dao = noteDao ?: return@withContext BookResult.ValidationError(ValidationReason.NOTE_TEXT_BLANK)
        val book = bookDao.getById(bookId)
            ?: return@withContext BookResult.ValidationError(ValidationReason.TITLE_BLANK)

        val validation = validator.validateNote(page, text, book.totalPages)
        if (validation is ValidationResult.Invalid) {
            return@withContext BookResult.ValidationError(validation.reason)
        }

        val note = Note(
            bookId = bookId,
            page = page,
            text = text.trim(),
            createdAt = System.currentTimeMillis()
        )
        val id = dao.insert(note)
        onDataChanged?.invoke()
        BookResult.Success(id)
    }

    override suspend fun editNote(note: Note): BookResult<Unit> = withContext(Dispatchers.IO) {
        val dao = noteDao ?: return@withContext BookResult.ValidationError(ValidationReason.NOTE_TEXT_BLANK)
        val book = bookDao.getById(note.bookId)
            ?: return@withContext BookResult.ValidationError(ValidationReason.TITLE_BLANK)

        val validation = validator.validateNote(note.page, note.text, book.totalPages)
        if (validation is ValidationResult.Invalid) {
            return@withContext BookResult.ValidationError(validation.reason)
        }

        dao.update(note.copy(text = note.text.trim()))
        onDataChanged?.invoke()
        BookResult.Success(Unit)
    }

    override suspend fun deleteNote(note: Note): Unit = withContext(Dispatchers.IO) {
        noteDao?.delete(note)
        onDataChanged?.invoke()
    }

    override fun observeNotes(bookId: Long): Flow<List<Note>> {
        return noteDao?.observeForBook(bookId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    private suspend fun reconcileBookStatus(bookId: Long) {
        val book = bookDao.getById(bookId) ?: return
        val lastSession = sessionDao.getLastForBook(bookId)
        if (lastSession != null && lastSession.endPage == book.totalPages) {
            if (book.status != BookStatus.FINISHED) {
                bookDao.update(book.copy(status = BookStatus.FINISHED))
            }
        } else {
            if (book.status == BookStatus.FINISHED) {
                bookDao.update(book.copy(status = BookStatus.READING))
            }
        }
    }
}
