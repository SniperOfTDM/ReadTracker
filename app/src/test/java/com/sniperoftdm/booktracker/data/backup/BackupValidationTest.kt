package com.sniperoftdm.booktracker.data.backup

import com.sniperoftdm.booktracker.domain.SessionValidator
import com.sniperoftdm.booktracker.domain.ValidationReason
import com.sniperoftdm.booktracker.domain.ValidationResult
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val validator = SessionValidator()

    @Test
    fun serializeAndDeserialize_payload_preservesAllFields() {
        val payload = BackupPayload(
            schemaVersion = 1,
            exportedAt = 1700000000000L,
            books = listOf(
                BackupBook(
                    exportId = "book-1",
                    title = "Kotlin in Action",
                    author = "Dmitry Jemerov",
                    totalPages = 360,
                    startPage = 0,
                    status = "READING",
                    goalDateEpochDay = 20000L,
                    createdAt = 1700000000000L
                )
            ),
            sessions = listOf(
                BackupSession(
                    exportId = "sess-1",
                    bookExportId = "book-1",
                    timestamp = 1700001000000L,
                    endPage = 45,
                    durationSeconds = 1800
                )
            ),
            notes = listOf(
                BackupNote(
                    exportId = "note-1",
                    bookExportId = "book-1",
                    page = 20,
                    text = "Extensions are resolved statically.",
                    createdAt = 1700001200000L
                )
            ),
            settings = BackupSettings(
                countZeroDays = true,
                paceWindowDays = 7,
                yearlyGoalBooks = 20,
                reminderEnabled = true,
                reminderHour = 21,
                reminderMinute = 30
            )
        )

        val jsonString = json.encodeToString(BackupPayload.serializer(), payload)
        val deserialized = json.decodeFromString(BackupPayload.serializer(), jsonString)

        assertEquals(1, deserialized.schemaVersion)
        assertEquals(1700000000000L, deserialized.exportedAt)
        assertEquals(1, deserialized.books.size)
        assertEquals("Kotlin in Action", deserialized.books.first().title)
        assertEquals(1, deserialized.sessions.size)
        assertEquals(1800, deserialized.sessions.first().durationSeconds)
        assertEquals(1, deserialized.notes.size)
        assertEquals(20, deserialized.notes.first().page)
        assertEquals(20, deserialized.settings?.yearlyGoalBooks)
    }

    @Test
    fun validator_detectsBlankBookTitle() {
        val result = validator.validateBook(
            title = "   ",
            totalPages = 200,
            startPage = 0
        )
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(ValidationReason.TITLE_BLANK, (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun validator_detectsInvalidNotePage() {
        val resultNegative = validator.validateNote(
            page = -1,
            text = "Valid note",
            bookTotalPages = 200
        )
        assertTrue(resultNegative is ValidationResult.Invalid)
        assertEquals(ValidationReason.NOTE_PAGE_OUT_OF_RANGE, (resultNegative as ValidationResult.Invalid).reason)

        val resultExceed = validator.validateNote(
            page = 201,
            text = "Valid note",
            bookTotalPages = 200
        )
        assertTrue(resultExceed is ValidationResult.Invalid)
        assertEquals(ValidationReason.NOTE_PAGE_OUT_OF_RANGE, (resultExceed as ValidationResult.Invalid).reason)

        val resultBlankText = validator.validateNote(
            page = 50,
            text = "   ",
            bookTotalPages = 200
        )
        assertTrue(resultBlankText is ValidationResult.Invalid)
        assertEquals(ValidationReason.NOTE_TEXT_BLANK, (resultBlankText as ValidationResult.Invalid).reason)
    }

    @Test
    fun schemaVersion2_roundTripsWithIsbnAndOpenLibraryKey() {
        val payload = BackupPayload(
            schemaVersion = 2,
            exportedAt = 1710000000000L,
            books = listOf(
                BackupBook(
                    exportId = "book-v2-1",
                    title = "Refactoring",
                    author = "Martin Fowler",
                    totalPages = 448,
                    startPage = 0,
                    status = "READING",
                    goalDateEpochDay = 20500L,
                    createdAt = 1710000000000L,
                    isbn = "9780134757599",
                    openLibraryKey = "/works/OL18146743W"
                )
            )
        )

        val jsonString = json.encodeToString(BackupPayload.serializer(), payload)
        val deserialized = json.decodeFromString(BackupPayload.serializer(), jsonString)

        assertEquals(2, deserialized.schemaVersion)
        assertEquals(1, deserialized.books.size)
        val book = deserialized.books.first()
        assertEquals("Refactoring", book.title)
        assertEquals("9780134757599", book.isbn)
        assertEquals("/works/OL18146743W", book.openLibraryKey)
    }

    @Test
    fun schemaVersion1_importCompatibility_defaultsNewFieldsToNull() {
        val v1Json = """
            {
                "schemaVersion": 1,
                "exportedAt": 1690000000000,
                "books": [
                    {
                        "exportId": "legacy-1",
                        "title": "Clean Code",
                        "author": "Robert C. Martin",
                        "totalPages": 464,
                        "startPage": 0,
                        "status": "FINISHED",
                        "goalDateEpochDay": null,
                        "createdAt": 1690000000000
                    }
                ],
                "sessions": [],
                "notes": []
            }
        """.trimIndent()

        val deserialized = json.decodeFromString(BackupPayload.serializer(), v1Json)
        assertEquals(1, deserialized.schemaVersion)
        assertEquals(1, deserialized.books.size)
        val book = deserialized.books.first()
        assertEquals("Clean Code", book.title)
        assertEquals(null, book.isbn)
        assertEquals(null, book.openLibraryKey)
    }
}
