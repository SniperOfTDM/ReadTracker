package com.sniperoftdm.booktracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Session
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {

    private lateinit var database: BookTrackerDatabase
    private lateinit var bookDao: BookDao
    private lateinit var sessionDao: SessionDao
    private var testBookId: Long = 0L

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BookTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bookDao = database.bookDao()
        sessionDao = database.sessionDao()

        val book = Book(
            title = "Test Book",
            author = "Author",
            totalPages = 300,
            startPage = 0,
            status = BookStatus.READING,
            createdAt = 1000L
        )
        testBookId = bookDao.insert(book)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun getPreviousAndNextSession() = runBlocking {
        val s1 = Session(bookId = testBookId, timestamp = 1000L, endPage = 20)
        val s2 = Session(bookId = testBookId, timestamp = 3000L, endPage = 50)
        val s3 = Session(bookId = testBookId, timestamp = 5000L, endPage = 80)

        val id1 = sessionDao.insert(s1)
        val id2 = sessionDao.insert(s2)
        val id3 = sessionDao.insert(s3)

        // Previous session to s2 (timestamp 3000L) should be s1
        val prevOf2 = sessionDao.getPreviousSession(testBookId, 3000L, id2)
        assertNotNull(prevOf2)
        assertEquals(id1, prevOf2?.id)
        assertEquals(20, prevOf2?.endPage)

        // Next session to s2 should be s3
        val nextOf2 = sessionDao.getNextSession(testBookId, 3000L, id2)
        assertNotNull(nextOf2)
        assertEquals(id3, nextOf2?.id)
        assertEquals(80, nextOf2?.endPage)

        // Check boundary: no previous for s1
        val prevOf1 = sessionDao.getPreviousSession(testBookId, 1000L, id1)
        assertNull(prevOf1)

        // Check boundary: no next for s3
        val nextOf3 = sessionDao.getNextSession(testBookId, 5000L, id3)
        assertNull(nextOf3)
    }

    @Test
    fun getLastForBookReturnsMostRecent() = runBlocking {
        val s1 = Session(bookId = testBookId, timestamp = 1000L, endPage = 20)
        val s2 = Session(bookId = testBookId, timestamp = 2000L, endPage = 45)
        sessionDao.insert(s1)
        val id2 = sessionDao.insert(s2)

        val last = sessionDao.getLastForBook(testBookId)
        assertNotNull(last)
        assertEquals(id2, last?.id)
        assertEquals(45, last?.endPage)
    }
}
