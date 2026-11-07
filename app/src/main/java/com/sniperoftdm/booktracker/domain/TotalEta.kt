package com.sniperoftdm.booktracker.domain

import java.time.LocalDate

/**
 * Projected completion metrics across all currently reading books sharing a global reading pace.
 */
data class TotalEta(
    val daysLeft: Int,
    val finishDate: LocalDate,
    val sharedPace: Double
)
