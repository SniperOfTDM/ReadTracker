package com.sniperoftdm.booktracker.domain

import java.time.LocalDate

/**
 * Immutable snapshot of reading statistics aggregated across all books.
 */
data class GlobalStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val bestDay: Pair<LocalDate, Int>?,
    val totalPagesRead: Int,
    val pagesPerDay: Map<LocalDate, Int>,
    val pagesPerHour: Double? = null
)
