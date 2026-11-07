package com.sniperoftdm.booktracker.domain

/**
 * Immutable snapshot of yearly reading goal progress and schedule variance.
 */
data class YearlyGoalProgress(
    val finishedCount: Int,
    val goal: Int,
    val progressFraction: Float,
    val aheadBehindSchedule: Double
)
