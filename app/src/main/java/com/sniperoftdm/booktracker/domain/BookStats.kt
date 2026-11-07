package com.sniperoftdm.booktracker.domain

import com.sniperoftdm.booktracker.data.model.Session
import java.time.LocalDate

/**
 * Immutable snapshot of all computed statistics for a single book.
 */
data class BookStats(
    val currentPage: Int,
    val pagesLeft: Int,
    val progressPercent: Float,
    val todayPages: Int,
    val dailyAverageAllTime: Double,
    val dailyAverageRecent: Double,
    val estimatedDaysLeft: Int?,
    val estimatedFinishDate: LocalDate?,
    val requiredPagesPerDay: Int?,
    val isOnTrack: Boolean?,
    val pagesPerDay: Map<LocalDate, Int>,
    val sessionsWithPages: List<Pair<Session, Int>>,
    val pagesPerHour: Double? = null,
    val totalReadingSeconds: Long = 0L
)
