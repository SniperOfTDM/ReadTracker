package com.sniperoftdm.booktracker.domain

/**
 * Configuration that controls how reading statistics are calculated.
 */
data class StatsConfig(
    val countZeroDays: Boolean = true,
    val paceWindowDays: Int = 7
)
