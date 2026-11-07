package com.sniperoftdm.booktracker.domain

/**
 * Result of a domain validation check.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val reason: ValidationReason) : ValidationResult
}

/**
 * Enumeration of all possible validation failure reasons.
 */
enum class ValidationReason {
    END_PAGE_NOT_GREATER_THAN_PREVIOUS,
    END_PAGE_EXCEEDS_TOTAL,
    END_PAGE_NOT_LESS_THAN_NEXT,
    TIMESTAMP_IN_FUTURE,
    TITLE_BLANK,
    TOTAL_PAGES_NOT_POSITIVE,
    START_PAGE_OUT_OF_RANGE,
    TOTAL_PAGES_BELOW_CURRENT,
    NOTE_TEXT_BLANK,
    NOTE_PAGE_OUT_OF_RANGE
}
