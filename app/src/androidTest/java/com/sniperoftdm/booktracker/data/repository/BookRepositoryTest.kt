package com.sniperoftdm.booktracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sniperoftdm.booktracker.data.local.BookDao
import com.sniperoftdm.booktracker.data.local.BookTrackerDatabase
import com.sniperoftdm.booktracker.data.local.SessionDao
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.domain.SessionValidator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookRepositoryTest {

    private lateinit var database: BookTrackerDatabase
    private lateinit var bookDao: BookDao
    private lateinit var sessionDao: SessionDao
    private lateinit var repository: BookRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BookTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bookDao = database.bookDao()
        sessionDao = database.sessionDao()
        repository = BookRepositoryImpl(
            bookDao = bookDao,
            sessionDao = sessionDao,
            validator = SessionValidator()
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun bookAutomaticallyBecomesFinishedWhenFinalPageIsReached() = runBlocking {
        val addResult = repository.addBook(
            title = "The Pragmatic Programmer",
            author = "Andy Hunt",
            totalPages = 100,
            startPage = 0
        )
        assertTrue(addResult is BookResult.Success)
        val bookId = (addResult as BookResult.Success).data

        // Initial status is READING
        val bookBefore = bookDao.getById(bookId)
        assertEquals(BookStatus.READING, bookBefore?.status)

        // Log checkpoint reaching totalPages (100)
        val logResult = repository.logCheckpoint(bookId = bookId, endPage = 100, timestamp = 1000L)
        assertTrue(logResult is BookResult.Success)

        // Status must automatically switch to FINISHED
        val bookAfter = bookDao.getById(bookId)
        assertNotNull(bookAfter)
        assertEquals(BookStatus.FINISHED, bookAfter?.status)
    }

    @Test
    fun bookRevertsToReadingWhenFinishingSessionIsEditedOrDeleted() = runBlocking {
        val addResult = repository.addBook(
            title = "Clean Code",
            author = "Robert C. Martin",
            totalPages = 100,
            startPage = 0
        )
        val bookId = (addResult as BookResult.Success).data

        // Finish the book
        val log1 = repository.logCheckpoint(bookId = bookId, endPage = 50, timestamp = 1000L)
        val log2 = repository.logCheckpoint(bookId = bookId, endPage = 100, timestamp = 2000L)
        val session2Id = (log2 as BookResult.Success).data

        assertEquals(BookStatus.FINISHED, bookDao.getById(bookId)?.status)

        // Edit session2 from 100 down to 80
        val session2 = sessionDao.getById(session2Id)!!
        val editResult = repository.editSession(session2.copy(endPage = 80))
        assertTrue(editResult is BookResult.Success)

        // Status must revert back to READING
        assertEquals(BookStatus.READING, bookDao.getById(bookId)?.status)

        // Log 100 again to finish
        val log3 = repository.logCheckpoint(bookId = bookId, endPage = 100, timestamp = 3000L)
        val session3Id = (log3 as BookResult.Success).data
        assertEquals(BookStatus.FINISHED, bookDao.getById(bookId)?.status)

        // Now delete session 3
        val session3 = sessionDao.getById(session3Id)!!
        repository.deleteSession(session3)

        // Status must revert to READING again
        assertEquals(BookStatus.READING, bookDao.getById(bookId)?.status)
    }

    @Test
    fun deletingBookCascadesToSessionsViaRepository() = runBlocking {
        val addResult = repository.addBook(
            title = "Refactoring",
            author = "Martin Fowler",
            totalPages = 200,
            startPage = 0
        )
        val bookId = (addResult as BookResult.Success).data
        repository.logCheckpoint(bookId = bookId, endPage = 40, timestamp = 1000L)
        repository.logCheckpoint(bookId = bookId, endPage = 90, timestamp = 2000L)

        val sessions = repository.observeSessions(bookId).first()
        assertEquals(2, sessions.size)

        val book = bookDao.getById(bookId)!!
        repository.deleteBook(book)

        val booksAfter = repository.observeBooks().first()
        assertTrue(booksAfter.none { it.id == bookId })

        val sessionsAfter = repository.observeSessions(bookId).first()
        assertTrue(sessionsAfter.isEmpty())
    }
}
