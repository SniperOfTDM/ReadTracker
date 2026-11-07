package com.sniperoftdm.booktracker.data.backup

import androidx.room.withTransaction
import com.sniperoftdm.booktracker.data.local.BookTrackerDatabase
import com.sniperoftdm.booktracker.data.local.CoverStorage
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Note
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.data.repository.SettingsRepository
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.domain.SessionValidator
import com.sniperoftdm.booktracker.domain.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BackupRepositoryImpl(
    private val database: BookTrackerDatabase,
    private val settingsRepository: SettingsRepository,
    private val coverStorage: CoverStorage? = null,
    private val validator: SessionValidator = SessionValidator(),
    private val onDataChanged: (() -> Unit)? = null
) : BackupRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    override suspend fun export(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val books = database.bookDao().observeAll().first()
            val sessions = database.sessionDao().observeAll().first()
            val notes = database.noteDao().observeAll().first()
            val settings = settingsRepository.settings.first()

            val bookIdToExportId = books.associate { it.id to UUID.randomUUID().toString() }

            val backupBooks = books.map { book ->
                BackupBook(
                    exportId = bookIdToExportId[book.id] ?: UUID.randomUUID().toString(),
                    title = book.title,
                    author = book.author,
                    totalPages = book.totalPages,
                    startPage = book.startPage,
                    status = book.status.name,
                    goalDateEpochDay = book.goalDateEpochDay,
                    createdAt = book.createdAt,
                    isbn = book.isbn,
                    openLibraryKey = book.openLibraryKey
                )
            }

            val backupSessions = sessions.mapNotNull { session ->
                val bookExportId = bookIdToExportId[session.bookId] ?: return@mapNotNull null
                BackupSession(
                    exportId = UUID.randomUUID().toString(),
                    bookExportId = bookExportId,
                    timestamp = session.timestamp,
                    endPage = session.endPage,
                    durationSeconds = session.durationSeconds
                )
            }

            val backupNotes = notes.mapNotNull { note ->
                val bookExportId = bookIdToExportId[note.bookId] ?: return@mapNotNull null
                BackupNote(
                    exportId = UUID.randomUUID().toString(),
                    bookExportId = bookExportId,
                    page = note.page,
                    text = note.text,
                    createdAt = note.createdAt
                )
            }

            val payload = BackupPayload(
                schemaVersion = 2,
                exportedAt = System.currentTimeMillis(),
                books = backupBooks,
                sessions = backupSessions,
                notes = backupNotes,
                settings = BackupSettings(
                    countZeroDays = settings.countZeroDays,
                    paceWindowDays = settings.paceWindowDays,
                    yearlyGoalBooks = settings.yearlyGoalBooks,
                    reminderEnabled = settings.reminderEnabled,
                    reminderHour = settings.reminderHour,
                    reminderMinute = settings.reminderMinute
                )
            )

            val jsonString = json.encodeToString(BackupPayload.serializer(), payload)
            outputStream.bufferedWriter().use { it.write(jsonString) }
        }
    }

    override suspend fun import(inputStream: InputStream, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        val jsonText = try {
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return@withContext ImportResult.Error("Failed to read input stream: ${e.message}")
        }

        val payload = try {
            json.decodeFromString(BackupPayload.serializer(), jsonText)
        } catch (e: Exception) {
            return@withContext ImportResult.Error("Malformed JSON: ${e.message}")
        }

        if (payload.schemaVersion !in 1..2) {
            return@withContext ImportResult.Error("Unsupported schemaVersion: ${payload.schemaVersion}")
        }

        // Validate all records before modifying the database
        val problems = mutableListOf<ImportProblem>()
        val now = System.currentTimeMillis()
        val booksByExportId = payload.books.associateBy { it.exportId }

        for (book in payload.books) {
            val bookValidation = validator.validateBook(
                title = book.title,
                totalPages = book.totalPages,
                startPage = book.startPage
            )
            if (bookValidation is ValidationResult.Invalid) {
                problems.add(ImportProblem(book.title.ifBlank { "Untitled" }, bookValidation.reason.name))
            }
        }

        val sessionsByBookExportId = payload.sessions.groupBy { it.bookExportId }
        for ((bookExportId, sessions) in sessionsByBookExportId) {
            val book = booksByExportId[bookExportId]
            if (book == null) {
                problems.add(ImportProblem("Unknown Book ($bookExportId)", "REFERENCED_BOOK_NOT_FOUND"))
                continue
            }

            val domainBook = Book(
                id = 0L,
                title = book.title,
                author = book.author,
                totalPages = book.totalPages,
                startPage = book.startPage,
                status = BookStatus.valueOf(book.status.ifBlank { "READING" })
            )

            val sortedSessions = sessions.sortedWith(compareBy({ it.timestamp }, { it.exportId }))
            var previousSession: Session? = null

            for (i in sortedSessions.indices) {
                val current = sortedSessions[i]
                val next = if (i + 1 < sortedSessions.size) sortedSessions[i + 1] else null
                val nextSession = next?.let {
                    Session(id = 0L, bookId = 0L, timestamp = it.timestamp, endPage = it.endPage)
                }

                val result = validator.validateNewSession(
                    endPage = current.endPage,
                    timestamp = current.timestamp,
                    book = domainBook,
                    previousSession = previousSession,
                    nextSession = nextSession,
                    currentTimeMillis = now
                )

                if (result is ValidationResult.Invalid) {
                    problems.add(ImportProblem(book.title, result.reason.name))
                }

                previousSession = Session(
                    id = 0L,
                    bookId = 0L,
                    timestamp = current.timestamp,
                    endPage = current.endPage,
                    durationSeconds = current.durationSeconds
                )
            }
        }

        for (note in payload.notes) {
            val book = booksByExportId[note.bookExportId]
            if (book == null) {
                problems.add(ImportProblem("Unknown Book (${note.bookExportId})", "REFERENCED_BOOK_NOT_FOUND"))
                continue
            }
            val noteResult = validator.validateNote(note.page, note.text, book.totalPages)
            if (noteResult is ValidationResult.Invalid) {
                problems.add(ImportProblem(book.title, noteResult.reason.name))
            }
        }

        if (problems.isNotEmpty()) {
            return@withContext ImportResult.ValidationError(ImportReport(problems))
        }

        // Clear covers folder on REPLACE_ALL import
        if (mode == ImportMode.REPLACE_ALL) {
            coverStorage?.clearAll()
        }

        // Execution in a single atomic transaction
        database.withTransaction {
            val existingBooks = database.bookDao().observeAll().first()
            for (book in existingBooks) {
                database.bookDao().delete(book)
            }

            val exportIdToRoomId = mutableMapOf<String, Long>()
            for (book in payload.books) {
                val domainBook = Book(
                    title = book.title.trim(),
                    author = book.author.trim(),
                    totalPages = book.totalPages,
                    startPage = book.startPage,
                    status = try {
                        BookStatus.valueOf(book.status)
                    } catch (e: Exception) {
                        BookStatus.READING
                    },
                    goalDateEpochDay = book.goalDateEpochDay,
                    createdAt = book.createdAt,
                    isbn = book.isbn,
                    openLibraryKey = book.openLibraryKey,
                    coverPath = null
                )
                val newId = database.bookDao().insert(domainBook)
                exportIdToRoomId[book.exportId] = newId
            }

            for (session in payload.sessions) {
                val newBookId = exportIdToRoomId[session.bookExportId] ?: continue
                val domainSession = Session(
                    bookId = newBookId,
                    timestamp = session.timestamp,
                    endPage = session.endPage,
                    durationSeconds = session.durationSeconds
                )
                database.sessionDao().insert(domainSession)
            }

            for (note in payload.notes) {
                val newBookId = exportIdToRoomId[note.bookExportId] ?: continue
                val domainNote = Note(
                    bookId = newBookId,
                    page = note.page,
                    text = note.text.trim(),
                    createdAt = note.createdAt
                )
                database.noteDao().insert(domainNote)
            }

            // Reconcile status for imported books
            for ((_, newBookId) in exportIdToRoomId) {
                val book = database.bookDao().getById(newBookId) ?: continue
                val lastSession = database.sessionDao().getLastForBook(newBookId)
                if (lastSession != null && lastSession.endPage == book.totalPages) {
                    if (book.status != BookStatus.FINISHED) {
                        database.bookDao().update(book.copy(status = BookStatus.FINISHED))
                    }
                } else if (book.status == BookStatus.FINISHED) {
                    database.bookDao().update(book.copy(status = BookStatus.READING))
                }
            }

            // Restore settings if present
            if (payload.settings != null) {
                settingsRepository.updateSettings(
                    UserSettings(
                        countZeroDays = payload.settings.countZeroDays,
                        paceWindowDays = payload.settings.paceWindowDays,
                        yearlyGoalBooks = payload.settings.yearlyGoalBooks,
                        reminderEnabled = payload.settings.reminderEnabled,
                        reminderHour = payload.settings.reminderHour,
                        reminderMinute = payload.settings.reminderMinute
                    )
                )
            }
        }

        onDataChanged?.invoke()
        ImportResult.Success(
            booksImported = payload.books.size,
            sessionsImported = payload.sessions.size,
            notesImported = payload.notes.size
        )
    }
}
