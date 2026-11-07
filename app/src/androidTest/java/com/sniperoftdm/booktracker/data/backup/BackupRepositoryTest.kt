package com.sniperoftdm.booktracker.data.backup

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sniperoftdm.booktracker.data.local.BookTrackerDatabase
import com.sniperoftdm.booktracker.data.local.CoverStorage
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Note
import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.data.repository.SettingsRepository
import com.sniperoftdm.booktracker.data.repository.SettingsRepositoryImpl
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.domain.SessionValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private lateinit var database: BookTrackerDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var backupRepository: BackupRepository
    private lateinit var coverStorage: CoverStorage

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        coverStorage = CoverStorage(context).apply { clearAll() }
        database = Room.inMemoryDatabaseBuilder(context, BookTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + Job()),
            produceFile = { File(context.filesDir, "test_settings_${System.currentTimeMillis()}.preferences_pb") }
        )
        settingsRepository = SettingsRepositoryImpl(testDataStore)

        backupRepository = BackupRepositoryImpl(
            database = database,
            settingsRepository = settingsRepository,
            coverStorage = coverStorage,
            validator = SessionValidator()
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun roundTrip_exportsAndRestoresAllDataAndSettings() = runBlocking {
        // Seed database
        val book1Id = database.bookDao().insert(
            Book(title = "Clean Code", author = "Uncle Bob", totalPages = 400, startPage = 0, status = BookStatus.READING)
        )
        val book2Id = database.bookDao().insert(
            Book(title = "The Pragmatic Programmer", author = "Andy Hunt", totalPages = 350, startPage = 0, status = BookStatus.READING)
        )

        database.sessionDao().insert(Session(bookId = book1Id, timestamp = 1000L, endPage = 50, durationSeconds = 1800))
        database.sessionDao().insert(Session(bookId = book1Id, timestamp = 2000L, endPage = 100, durationSeconds = 3600))
        database.sessionDao().insert(Session(bookId = book2Id, timestamp = 1500L, endPage = 30, durationSeconds = null))

        database.noteDao().insert(Note(bookId = book1Id, page = 42, text = "Boy Scout rule.", createdAt = 1100L))
        database.noteDao().insert(Note(bookId = book2Id, page = 15, text = "Broken windows theory.", createdAt = 1600L))

        settingsRepository.updateSettings(
            UserSettings(
                countZeroDays = false,
                paceWindowDays = 14,
                yearlyGoalBooks = 25,
                reminderEnabled = true,
                reminderHour = 20,
                reminderMinute = 30
            )
        )

        // Export
        val outStream = ByteArrayOutputStream()
        val exportResult = backupRepository.export(outStream)
        assertTrue(exportResult.isSuccess)
        val exportedJson = outStream.toByteArray()
        assertTrue(exportedJson.isNotEmpty())

        // Clear DB and reset settings
        database.bookDao().delete(database.bookDao().getById(book1Id)!!)
        database.bookDao().delete(database.bookDao().getById(book2Id)!!)
        settingsRepository.updateSettings(UserSettings())

        assertEquals(0, database.bookDao().observeAll().first().size)
        assertEquals(0, database.sessionDao().observeAll().first().size)
        assertEquals(0, database.noteDao().observeAll().first().size)

        // Import
        val inStream = ByteArrayInputStream(exportedJson)
        val importResult = backupRepository.import(inStream)
        assertTrue(importResult is ImportResult.Success)
        val success = importResult as ImportResult.Success
        assertEquals(2, success.booksImported)
        assertEquals(3, success.sessionsImported)
        assertEquals(2, success.notesImported)

        // Verify restoration
        val restoredBooks = database.bookDao().observeAll().first()
        assertEquals(2, restoredBooks.size)

        val restoredCleanCode = restoredBooks.find { it.title == "Clean Code" }
        assertNotNull(restoredCleanCode)
        assertEquals("Uncle Bob", restoredCleanCode?.author)
        assertEquals(400, restoredCleanCode?.totalPages)

        val cleanCodeSessions = database.sessionDao().observeForBook(restoredCleanCode!!.id).first()
        assertEquals(2, cleanCodeSessions.size)
        assertEquals(50, cleanCodeSessions[0].endPage)
        assertEquals(100, cleanCodeSessions[1].endPage)

        val cleanCodeNotes = database.noteDao().observeForBook(restoredCleanCode.id).first()
        assertEquals(1, cleanCodeNotes.size)
        assertEquals(42, cleanCodeNotes[0].page)
        assertEquals("Boy Scout rule.", cleanCodeNotes[0].text)

        val restoredSettings = settingsRepository.settings.first()
        assertEquals(false, restoredSettings.countZeroDays)
        assertEquals(14, restoredSettings.paceWindowDays)
        assertEquals(25, restoredSettings.yearlyGoalBooks)
        assertEquals(true, restoredSettings.reminderEnabled)
        assertEquals(20, restoredSettings.reminderHour)
        assertEquals(30, restoredSettings.reminderMinute)
    }

    @Test
    fun import_unsupportedSchemaVersion_failsAndWritesNothing() = runBlocking {
        val badSchemaJson = """
            {
                "schemaVersion": 999,
                "exportedAt": 1600000000000,
                "books": [
                    {
                        "exportId": "book-1",
                        "title": "Refactoring",
                        "author": "Martin Fowler",
                        "totalPages": 450,
                        "startPage": 0,
                        "status": "READING",
                        "createdAt": 1600000000000
                    }
                ],
                "sessions": [],
                "notes": []
            }
        """.trimIndent()

        val result = backupRepository.import(ByteArrayInputStream(badSchemaJson.toByteArray()))
        assertTrue(result is ImportResult.Error)
        assertEquals("Unsupported schemaVersion: 999", (result as ImportResult.Error).message)

        // Assert nothing written to database
        assertEquals(0, database.bookDao().observeAll().first().size)
    }

    @Test
    fun import_nonMonotonicPages_failsValidationAndWritesNothing() = runBlocking {
        val invalidPagesJson = """
            {
                "schemaVersion": 1,
                "exportedAt": 1600000000000,
                "books": [
                    {
                        "exportId": "book-1",
                        "title": "Design Patterns",
                        "author": "GoF",
                        "totalPages": 400,
                        "startPage": 0,
                        "status": "READING",
                        "createdAt": 1600000000000
                    }
                ],
                "sessions": [
                    {
                        "exportId": "sess-1",
                        "bookExportId": "book-1",
                        "timestamp": 1000,
                        "endPage": 50
                    },
                    {
                        "exportId": "sess-2",
                        "bookExportId": "book-1",
                        "timestamp": 2000,
                        "endPage": 30
                    }
                ],
                "notes": []
            }
        """.trimIndent()

        val result = backupRepository.import(ByteArrayInputStream(invalidPagesJson.toByteArray()))
        assertTrue(result is ImportResult.ValidationError)
        val validationError = result as ImportResult.ValidationError
        assertTrue(validationError.report.problems.any { it.reason == "END_PAGE_NOT_GREATER_THAN_PREVIOUS" })

        // Assert nothing written to database
        assertEquals(0, database.bookDao().observeAll().first().size)
        assertEquals(0, database.sessionDao().observeAll().first().size)
    }

    @Test
    fun import_blankTitle_failsValidationAndWritesNothing() = runBlocking {
        val blankTitleJson = """
            {
                "schemaVersion": 1,
                "exportedAt": 1600000000000,
                "books": [
                    {
                        "exportId": "book-1",
                        "title": "   ",
                        "author": "Unknown",
                        "totalPages": 200,
                        "startPage": 0,
                        "status": "READING",
                        "createdAt": 1600000000000
                    }
                ],
                "sessions": [],
                "notes": []
            }
        """.trimIndent()

        val result = backupRepository.import(ByteArrayInputStream(blankTitleJson.toByteArray()))
        assertTrue(result is ImportResult.ValidationError)
        val validationError = result as ImportResult.ValidationError
        assertTrue(validationError.report.problems.any { it.reason == "TITLE_BLANK" })

        // Assert nothing written to database
        assertEquals(0, database.bookDao().observeAll().first().size)
    }

    @Test
    fun import_garbageJson_failsAndWritesNothing() = runBlocking {
        val garbageJson = "{ this is completely malformed json syntax : 123 "

        val result = backupRepository.import(ByteArrayInputStream(garbageJson.toByteArray()))
        assertTrue(result is ImportResult.Error)
        val errorResult = result as ImportResult.Error
        assertTrue(errorResult.message.contains("Malformed JSON"))

        // Assert nothing written to database
        assertEquals(0, database.bookDao().observeAll().first().size)
    }

    @Test
    fun exportAndImport_roundTripsWithIsbnAndOpenLibraryKey() = runBlocking {
        val book = Book(
            id = 1L,
            title = "Refactoring",
            author = "Martin Fowler",
            totalPages = 448,
            startPage = 0,
            status = BookStatus.READING,
            isbn = "9780134757599",
            openLibraryKey = "/works/OL18146743W"
        )
        database.bookDao().insert(book)

        val out = ByteArrayOutputStream()
        val exportResult = backupRepository.export(out)
        assertTrue(exportResult.isSuccess)

        // Clear DB
        database.bookDao().delete(book)
        assertEquals(0, database.bookDao().observeAll().first().size)

        // Import
        val importResult = backupRepository.import(ByteArrayInputStream(out.toByteArray()))
        assertTrue(importResult is ImportResult.Success)

        val books = database.bookDao().observeAll().first()
        assertEquals(1, books.size)
        val imported = books.first()
        assertEquals("Refactoring", imported.title)
        assertEquals("9780134757599", imported.isbn)
        assertEquals("/works/OL18146743W", imported.openLibraryKey)
    }

    @Test
    fun importReplaceAll_clearsCoversFolder() = runBlocking {
        // Place a mock file in covers folder
        val dummyCover = File(coverStorage.coversDir, "dummy_999.jpg").apply {
            writeText("dummy cover bytes")
        }
        assertTrue(dummyCover.exists())

        // Create empty backup payload and import with REPLACE_ALL
        val emptyPayload = BackupPayload(
            schemaVersion = 2,
            exportedAt = System.currentTimeMillis()
        )
        val jsonText = kotlinx.serialization.json.Json.encodeToString(BackupPayload.serializer(), emptyPayload)
        val result = backupRepository.import(ByteArrayInputStream(jsonText.toByteArray()), ImportMode.REPLACE_ALL)
        assertTrue(result is ImportResult.Success)

        // Verify covers directory is cleared
        val remainingFiles = coverStorage.coversDir.listFiles() ?: emptyArray()
        assertEquals(0, remainingFiles.size)
    }
}
