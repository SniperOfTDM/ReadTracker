package com.sniperoftdm.booktracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextReminderCalculatorTest {

    private val utcZone = ZoneId.of("UTC")
    private val nyZone = ZoneId.of("America/New_York")

    @Test
    fun timeLaterTodayComputesSameDayDifference() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 3, 10),
            LocalTime.of(14, 30),
            utcZone
        )

        // Target: 21:00 today -> 6 hours and 30 minutes later
        val delay = NextReminderCalculator.delayUntilNext(now, 21, 0, utcZone)
        val expected = Duration.ofHours(6).plusMinutes(30)
        assertEquals(expected, delay)
    }

    @Test
    fun timeAlreadyPassedTodayComputesNextDayDifference() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 3, 10),
            LocalTime.of(22, 15),
            utcZone
        )

        // Target: 21:00 (already passed today) -> tomorrow at 21:00 -> 22 hours and 45 minutes
        val delay = NextReminderCalculator.delayUntilNext(now, 21, 0, utcZone)
        val expected = Duration.ofHours(22).plusMinutes(45)
        assertEquals(expected, delay)
    }

    @Test
    fun exactlyNowTargetsTomorrowAtSameTime() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 3, 10),
            LocalTime.of(21, 0),
            utcZone
        )

        // Target: 21:00 -> scheduled for tomorrow at 21:00 (24 hours)
        val delay = NextReminderCalculator.delayUntilNext(now, 21, 0, utcZone)
        val expected = Duration.ofHours(24)
        assertEquals(expected, delay)
    }

    @Test
    fun handlesDaylightSavingTimeTransitionSpringForwardAndFallBack() {
        // In America/New_York, Spring forward occurs on March 8, 2026 (clock jumps 02:00 -> 03:00)
        // From March 7 at 21:00 to March 8 at 21:00 is a 23-hour calendar day
        val springNow = ZonedDateTime.of(
            LocalDate.of(2026, 3, 7),
            LocalTime.of(21, 0),
            nyZone
        )
        val springDelay = NextReminderCalculator.delayUntilNext(springNow, 21, 0, nyZone)
        assertEquals(Duration.ofHours(23), springDelay)

        // In America/New_York, Fall back occurs on November 1, 2026 (clock repeats 01:00)
        // From October 31 at 21:00 to November 1 at 21:00 is a 25-hour calendar day
        val fallNow = ZonedDateTime.of(
            LocalDate.of(2026, 10, 31),
            LocalTime.of(21, 0),
            nyZone
        )
        val fallDelay = NextReminderCalculator.delayUntilNext(fallNow, 21, 0, nyZone)
        assertEquals(Duration.ofHours(25), fallDelay)
    }
}
