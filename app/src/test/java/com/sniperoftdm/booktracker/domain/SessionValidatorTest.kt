package com.sniperoftdm.booktracker.domain

import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionValidatorTest {

    private val validator = SessionValidator()
    private val now = 1_000_000L

    private fun createBook(
        id: Long = 1L,
        title: String = "Effective Kotlin",
        totalPages: Int = 300,
        startPage: Int = 0
    ): Book {
        return Book(
            id = id,
            title = title,
            author = "Marcin Moskala",
            totalPages = totalPages,
            startPage = startPage,
            status = BookStatus.READING,
            goalDateEpochDay = null,
            createdAt = 500_000L
        )
    }

    @Test
    fun validFirstSessionReturnsValid() {
        val book = createBook(startPage = 10, totalPages = 200)
        val result = validator.validateNewSession(
            endPage = 25,
            timestamp = now - 1000L,
            book = book,
            previousSession = null,
            nextSession = null,
            currentTimeMillis = now
        )
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun endPageEqualOrLessThanPreviousReturnsInvalid() {
        val book = createBook(startPage = 10, totalPages = 200)

        // Equal to startPage on first session
        val equalResult = validator.validateNewSession(
            endPage = 10,
            timestamp = now - 1000L,
            book = book,
            previousSession = null,
            nextSession = null,
            currentTimeMillis = now
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS),
            equalResult
        )

        // Less than previous session endPage
        val prevSession = Session(id = 1L, bookId = book.id, timestamp = now - 2000L, endPage = 50)
        val lowerResult = validator.validateNewSession(
            endPage = 49,
            timestamp = now - 1000L,
            book = book,
            previousSession = prevSession,
            nextSession = null,
            currentTimeMillis = now
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS),
            lowerResult
        )
    }

    @Test
    fun endPageExceedingTotalPagesReturnsInvalid() {
        val book = createBook(startPage = 0, totalPages = 100)
        val result = validator.validateNewSession(
            endPage = 101,
            timestamp = now - 1000L,
            book = book,
            previousSession = null,
            nextSession = null,
            currentTimeMillis = now
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.END_PAGE_EXCEEDS_TOTAL),
            result
        )
    }

    @Test
    fun timestampInFutureReturnsInvalid() {
        val book = createBook(startPage = 0, totalPages = 100)
        val result = validator.validateNewSession(
            endPage = 20,
            timestamp = now + 5000L,
            book = book,
            previousSession = null,
            nextSession = null,
            currentTimeMillis = now
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.TIMESTAMP_IN_FUTURE),
            result
        )
    }

    @Test
    fun validBackfillBetweenTwoSessionsReturnsValid() {
        val book = createBook(startPage = 0, totalPages = 200)
        val prevSession = Session(id = 1L, bookId = book.id, timestamp = now - 5000L, endPage = 20)
        val nextSession = Session(id = 2L, bookId = book.id, timestamp = now - 1000L, endPage = 60)

        val backfillResult = validator.validateNewSession(
            endPage = 40,
            timestamp = now - 3000L,
            book = book,
            previousSession = prevSession,
            nextSession = nextSession,
            currentTimeMillis = now
        )
        assertTrue(backfillResult is ValidationResult.Valid)
    }

    @Test
    fun invalidBackfillWhereEndPageBreaksMonotonicity() {
        val book = createBook(startPage = 0, totalPages = 200)
        val prevSession = Session(id = 1L, bookId = book.id, timestamp = now - 5000L, endPage = 20)
        val nextSession = Session(id = 2L, bookId = book.id, timestamp = now - 1000L, endPage = 60)

        // Equal to or greater than next session's endPage
        val exceedNext = validator.validateNewSession(
            endPage = 60,
            timestamp = now - 3000L,
            book = book,
            previousSession = prevSession,
            nextSession = nextSession,
            currentTimeMillis = now
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.END_PAGE_NOT_LESS_THAN_NEXT),
            exceedNext
        )

        // Equal to or lower than previous session's endPage
        val lowerThanPrev = validator.validateNewSession(
            endPage = 20,
            timestamp = now - 3000L,
            book = book,
            previousSession = prevSession,
            nextSession = nextSession,
            currentTimeMillis = now
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS),
            lowerThanPrev
        )
    }

    @Test
    fun editingMiddleSessionValidAndInvalidCases() {
        val book = createBook(startPage = 0, totalPages = 200)
        val session1 = Session(id = 1L, bookId = book.id, timestamp = now - 5000L, endPage = 20)
        val session2 = Session(id = 2L, bookId = book.id, timestamp = now - 3000L, endPage = 40)
        val session3 = Session(id = 3L, bookId = book.id, timestamp = now - 1000L, endPage = 70)

        // Valid edit: changed to 50 (between 20 and 70)
        val validEdit = session2.copy(endPage = 50)
        val validResult = validator.validateEditSession(
            session = validEdit,
            book = book,
            previousSession = session1,
            nextSession = session3,
            currentTimeMillis = now
        )
        assertTrue(validResult is ValidationResult.Valid)

        // Invalid edit: <= session1 endPage (20)
        val invalidLowEdit = session2.copy(endPage = 19)
        val lowResult = validator.validateEditSession(
            session = invalidLowEdit,
            book = book,
            previousSession = session1,
            nextSession = session3,
            currentTimeMillis = now
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS),
            lowResult
        )

        // Invalid edit: >= session3 endPage (70)
        val invalidHighEdit = session2.copy(endPage = 75)
        val highResult = validator.validateEditSession(
            session = invalidHighEdit,
            book = book,
            previousSession = session1,
            nextSession = session3,
            currentTimeMillis = now
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.END_PAGE_NOT_LESS_THAN_NEXT),
            highResult
        )
    }

    @Test
    fun bookCreationValidationRules() {
        // Blank title
        assertEquals(
            ValidationResult.Invalid(ValidationReason.TITLE_BLANK),
            validator.validateBook("   ", 100, 0)
        )

        // Total pages <= 0
        assertEquals(
            ValidationResult.Invalid(ValidationReason.TOTAL_PAGES_NOT_POSITIVE),
            validator.validateBook("Title", 0, 0)
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.TOTAL_PAGES_NOT_POSITIVE),
            validator.validateBook("Title", -10, 0)
        )

        // Start page out of range
        assertEquals(
            ValidationResult.Invalid(ValidationReason.START_PAGE_OUT_OF_RANGE),
            validator.validateBook("Title", 100, -1)
        )
        assertEquals(
            ValidationResult.Invalid(ValidationReason.START_PAGE_OUT_OF_RANGE),
            validator.validateBook("Title", 100, 100)
        )

        // Valid book
        assertTrue(validator.validateBook("Valid Book", 250, 20) is ValidationResult.Valid)
    }

    @Test
    fun bookEditTotalPagesBelowCurrentPageReturnsInvalid() {
        val book = createBook(startPage = 0, totalPages = 200)
        val currentPage = 120

        // Attempting to lower totalPages below current page (120)
        val invalidEdit = book.copy(totalPages = 110)
        val result = validator.validateBookEdit(invalidEdit, currentPage)
        assertEquals(
            ValidationResult.Invalid(ValidationReason.TOTAL_PAGES_BELOW_CURRENT),
            result
        )

        // Setting totalPages equal to or higher than current page is valid
        val validEdit = book.copy(totalPages = 150)
        assertTrue(validator.validateBookEdit(validEdit, currentPage) is ValidationResult.Valid)
    }
}
