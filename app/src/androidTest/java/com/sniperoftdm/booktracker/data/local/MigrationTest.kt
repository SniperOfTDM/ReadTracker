package com.sniperoftdm.booktracker.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BookTrackerDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_preservesExistingData_and_supportsNotesWithCascadeDelete() {
        // Step 1: Create Database at version 1 with seed data
        var db = helper.createDatabase(testDb, 1)

        db.execSQL(
            """
            INSERT INTO books (id, title, author, totalPages, startPage, status, goalDateEpochDay, createdAt)
            VALUES (1, 'Atomic Habits', 'James Clear', 320, 0, 'READING', NULL, 1600000000000)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO sessions (id, bookId, timestamp, endPage, durationSeconds)
            VALUES (101, 1, 1600000500000, 45, 1800)
            """.trimIndent()
        )
        db.close()

        // Step 2: Migrate to version 2 and validate schema
        db = helper.runMigrationsAndValidate(testDb, 2, true, BookTrackerDatabase.MIGRATION_1_2)

        // Verify book data is preserved
        val bookCursor = db.query("SELECT id, title, author, totalPages, status FROM books WHERE id = 1")
        assertTrue(bookCursor.moveToFirst())
        assertEquals(1L, bookCursor.getLong(0))
        assertEquals("Atomic Habits", bookCursor.getString(1))
        assertEquals("James Clear", bookCursor.getString(2))
        assertEquals(320, bookCursor.getInt(3))
        assertEquals("READING", bookCursor.getString(4))
        bookCursor.close()

        // Verify session data is preserved
        val sessionCursor = db.query("SELECT id, bookId, endPage, durationSeconds FROM sessions WHERE id = 101")
        assertTrue(sessionCursor.moveToFirst())
        assertEquals(101L, sessionCursor.getLong(0))
        assertEquals(1L, sessionCursor.getLong(1))
        assertEquals(45, sessionCursor.getInt(2))
        assertEquals(1800, sessionCursor.getInt(3))
        sessionCursor.close()

        // Verify Note table works: insert a note for book 1
        db.execSQL(
            """
            INSERT INTO notes (id, bookId, page, text, createdAt)
            VALUES (201, 1, 42, 'Habits are the compound interest of self-improvement.', 1600001000000)
            """.trimIndent()
        )

        val noteCursor = db.query("SELECT id, bookId, page, text FROM notes WHERE id = 201")
        assertTrue(noteCursor.moveToFirst())
        assertEquals(201L, noteCursor.getLong(0))
        assertEquals(1L, noteCursor.getLong(1))
        assertEquals(42, noteCursor.getInt(2))
        assertEquals("Habits are the compound interest of self-improvement.", noteCursor.getString(3))
        noteCursor.close()

        // Verify foreign key cascade delete works
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM books WHERE id = 1")

        val remainingSessions = db.query("SELECT count(*) FROM sessions WHERE bookId = 1")
        assertTrue(remainingSessions.moveToFirst())
        assertEquals(0, remainingSessions.getInt(0))
        remainingSessions.close()

        val remainingNotes = db.query("SELECT count(*) FROM notes WHERE bookId = 1")
        assertTrue(remainingNotes.moveToFirst())
        assertEquals(0, remainingNotes.getInt(0))
        remainingNotes.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_addsIsbnOpenLibraryKeyCoverPath_preservesExistingData() {
        // Step 1: Create Database at version 2 with seed data
        var db = helper.createDatabase(testDb, 2)

        db.execSQL(
            """
            INSERT INTO books (id, title, author, totalPages, startPage, status, goalDateEpochDay, createdAt)
            VALUES (2, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 600, 0, 'READING', NULL, 1700000000000)
            """.trimIndent()
        )
        db.close()

        // Step 2: Migrate to version 3 using MIGRATION_2_3
        db = helper.runMigrationsAndValidate(testDb, 3, true, BookTrackerDatabase.MIGRATION_2_3)

        // Verify existing columns preserved and new columns exist with NULL values
        val cursor = db.query("SELECT id, title, author, totalPages, isbn, openLibraryKey, coverPath FROM books WHERE id = 2")
        assertTrue(cursor.moveToFirst())
        assertEquals(2L, cursor.getLong(0))
        assertEquals("Designing Data-Intensive Applications", cursor.getString(1))
        assertEquals("Martin Kleppmann", cursor.getString(2))
        assertEquals(600, cursor.getInt(3))
        assertTrue(cursor.isNull(4))
        assertTrue(cursor.isNull(5))
        assertTrue(cursor.isNull(6))
        cursor.close()

        // Verify updating the new columns works
        db.execSQL(
            """
            UPDATE books SET isbn = '9781449373320', openLibraryKey = '/works/OL17362309W', coverPath = 'covers/2.jpg'
            WHERE id = 2
            """.trimIndent()
        )
        val updatedCursor = db.query("SELECT isbn, openLibraryKey, coverPath FROM books WHERE id = 2")
        assertTrue(updatedCursor.moveToFirst())
        assertEquals("9781449373320", updatedCursor.getString(0))
        assertEquals("/works/OL17362309W", updatedCursor.getString(1))
        assertEquals("covers/2.jpg", updatedCursor.getString(2))
        updatedCursor.close()
    }
}
