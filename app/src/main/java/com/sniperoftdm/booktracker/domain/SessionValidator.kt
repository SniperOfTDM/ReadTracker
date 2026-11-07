package com.sniperoftdm.booktracker.domain

import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.Session

/**
 * Validates book and reading session domain invariants.
 */
class SessionValidator {

    /** Validates a newly proposed checkpoint session against the book and neighboring sessions. */
    fun validateNewSession(
        endPage: Int,
        timestamp: Long,
        book: Book,
        previousSession: Session?,
        nextSession: Session?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ValidationResult {
        if (timestamp > currentTimeMillis) {
            return ValidationResult.Invalid(ValidationReason.TIMESTAMP_IN_FUTURE)
        }

        val previousPage = previousSession?.endPage ?: book.startPage
        if (endPage <= previousPage) {
            return ValidationResult.Invalid(ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS)
        }

        if (endPage > book.totalPages) {
            return ValidationResult.Invalid(ValidationReason.END_PAGE_EXCEEDS_TOTAL)
        }

        if (nextSession != null && endPage >= nextSession.endPage) {
            return ValidationResult.Invalid(ValidationReason.END_PAGE_NOT_LESS_THAN_NEXT)
        }

        return ValidationResult.Valid
    }

    /** Validates an existing checkpoint session being updated with new values. */
    fun validateEditSession(
        session: Session,
        book: Book,
        previousSession: Session?,
        nextSession: Session?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ValidationResult {
        return validateNewSession(
            endPage = session.endPage,
            timestamp = session.timestamp,
            book = book,
            previousSession = previousSession,
            nextSession = nextSession,
            currentTimeMillis = currentTimeMillis
        )
    }

    /** Validates the initial configuration fields when creating a new book. */
    fun validateBook(
        title: String,
        totalPages: Int,
        startPage: Int
    ): ValidationResult {
        if (title.isBlank()) {
            return ValidationResult.Invalid(ValidationReason.TITLE_BLANK)
        }
        if (totalPages <= 0) {
            return ValidationResult.Invalid(ValidationReason.TOTAL_PAGES_NOT_POSITIVE)
        }
        if (startPage !in 0 until totalPages) {
            return ValidationResult.Invalid(ValidationReason.START_PAGE_OUT_OF_RANGE)
        }
        return ValidationResult.Valid
    }

    /** Validates updates to an existing book ensuring total pages does not fall below current progress. */
    fun validateBookEdit(
        book: Book,
        currentPage: Int
    ): ValidationResult {
        val baseValidation = validateBook(book.title, book.totalPages, book.startPage)
        if (baseValidation !is ValidationResult.Valid) {
            return baseValidation
        }
        if (book.totalPages < currentPage) {
            return ValidationResult.Invalid(ValidationReason.TOTAL_PAGES_BELOW_CURRENT)
        }
        return ValidationResult.Valid
    }

    /** Validates a note's text and page range against the book's total pages. */
    fun validateNote(
        page: Int,
        text: String,
        bookTotalPages: Int
    ): ValidationResult {
        if (text.isBlank()) {
            return ValidationResult.Invalid(ValidationReason.NOTE_TEXT_BLANK)
        }
        if (page !in 0..bookTotalPages) {
            return ValidationResult.Invalid(ValidationReason.NOTE_PAGE_OUT_OF_RANGE)
        }
        return ValidationResult.Valid
    }
}
