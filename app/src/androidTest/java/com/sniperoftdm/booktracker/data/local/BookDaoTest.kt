package com.sniperoftdm.booktracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Session
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDaoTest {

    private lateinit var database: BookTrackerDatabase
    private lateinit var bookDao: BookDao
    private lateinit var sessionDao: SessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BookTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bookDao = database.bookDao()
        sessionDao = database.sessionDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetBookById() = runBlocking {
        val book = Book(
            title = "Kotlin in Action",
            author = "Dmitry Jemerov",
            totalPages = 360,
            startPage = 0,
            status = BookStatus.READING,
            goalDateEpochDay = null,
            createdAt = 1000L
        )
        val id = bookDao.insert(book)
        val loaded = bookDao.getById(id)

        assertNotNull(loaded)
        assertEquals("Kotlin in Action", loaded?.title)
        assertEquals(360, loaded?.totalPages)
        assertEquals(BookStatus.READING, loaded?.status)
    }

    @Test
    fun observeByStatusFiltersCorrectly() = runBlocking {
        val reading = Book(
            title = "Book 1",
            author = "",
            totalPages = 200,
            startPage = 0,
            status = BookStatus.READING,
            createdAt = 1000L
        )
        val finished = Book(
            title = "Book 2",
            author = "",
            totalPages = 300,
            startPage = 0,
            status = BookStatus.FINISHED,
            createdAt = 2000L
        )
        bookDao.insert(reading)
        bookDao.insert(finished)

        val readingList = bookDao.observeByStatus(BookStatus.READING).first()
        val finishedList = bookDao.observeByStatus(BookStatus.FINISHED).first()

        assertEquals(1, readingList.size)
        assertEquals("Book 1", readingList.first().title)
        assertEquals(1, finishedList.size)
        assertEquals("Book 2", finishedList.first().title)
    }

    @Test
    fun deleteBookCascadesToSessions() = runBlocking {
        val book = Book(
            title = "Book with Sessions",
            author = "",
            totalPages = 200,
            startPage = 0,
            status = BookStatus.READING,
            createdAt = 1000L
        )
        val bookId = bookDao.insert(book)
        val session1 = Session(bookId = bookId, timestamp = 1000L, endPage = 20)
        val session2 = Session(bookId = bookId, timestamp = 2000L, endPage = 50)
        sessionDao.insert(session1)
        sessionDao.insert(session2)

        val sessionsBefore = sessionDao.getForBook(bookId)
        assertEquals(2, sessionsBefore.size)

        val savedBook = bookDao.getById(bookId)
        assertNotNull(savedBook)
        bookDao.delete(savedBook!!)

        val bookAfter = bookDao.getById(bookId)
        assertNull(bookAfter)

        val sessionsAfter = sessionDao.getForBook(bookId)
        assertTrue("Sessions should be deleted via CASCADE foreign key", sessionsAfter.isEmpty())
    }
}
