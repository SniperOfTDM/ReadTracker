package com.sniperoftdm.booktracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sniperoftdm.booktracker.data.local.BookTrackerDatabase
import com.sniperoftdm.booktracker.domain.SessionValidator
import com.sniperoftdm.booktracker.domain.ValidationReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import java.io.File

@RunWith(AndroidJUnit4::class)
class TimerRepositoryTest {

    private lateinit var database: BookTrackerDatabase
    private lateinit var bookRepository: BookRepository
    private lateinit var timerRepository: TimerRepository
    private var testBookId: Long = 0L

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BookTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        bookRepository = BookRepositoryImpl(
            bookDao = database.bookDao(),
            sessionDao = database.sessionDao(),
            noteDao = database.noteDao(),
            validator = SessionValidator()
        )

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + Job()),
            produceFile = { File(context.filesDir, "test_timer_${System.currentTimeMillis()}.preferences_pb") }
        )

        timerRepository = TimerRepositoryImpl(
            dataStore = testDataStore,
            bookRepository = bookRepository
        )

        val addResult = bookRepository.addBook("Timer Test Book", "Author", 200, 0)
        testBookId = (addResult as BookResult.Success).data
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun startTimerAndReplaceActiveTimerEnsuresOnlyOneRunning() = runBlocking {
        timerRepository.start(testBookId, 1000L)
        val active1 = timerRepository.observeActive().first()
        assertNotNull(active1)
        assertEquals(testBookId, active1?.bookId)
        assertEquals(1000L, active1?.startedAtMillis)

        // Start another timer for book 99L
        timerRepository.start(99L, 2000L)
        val active2 = timerRepository.observeActive().first()
        assertNotNull(active2)
        assertEquals(99L, active2?.bookId)
        assertEquals(2000L, active2?.startedAtMillis)
    }

    @Test
    fun stopAndLogValidCheckpointsSavesAndCancelsTimer() = runBlocking {
        timerRepository.start(testBookId, System.currentTimeMillis() - 5000L)

        val result = timerRepository.stopAndLog(endPage = 25)
        assertTrue(result is BookResult.Success)

        // Active timer should now be cleared
        val active = timerRepository.observeActive().first()
        assertNull(active)

        // Checkpoint should be in DB
        val sessions = bookRepository.observeSessions(testBookId).first()
        assertEquals(1, sessions.size)
        assertEquals(25, sessions.first().endPage)
        assertNotNull(sessions.first().durationSeconds)
        assertTrue(sessions.first().durationSeconds!! >= 5)
    }

    @Test
    fun stopAndLogInvalidKeepsTimerRunning() = runBlocking {
        val startedAt = System.currentTimeMillis() - 3000L
        timerRepository.start(testBookId, startedAt)

        // Invalid: endPage 0 is not greater than startPage 0
        val result = timerRepository.stopAndLog(endPage = 0)
        assertTrue(result is BookResult.ValidationError)
        assertEquals(
            ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS,
            (result as BookResult.ValidationError).reason
        )

        // Timer must still be running
        val active = timerRepository.observeActive().first()
        assertNotNull(active)
        assertEquals(testBookId, active?.bookId)
        assertEquals(startedAt, active?.startedAtMillis)
    }
}
