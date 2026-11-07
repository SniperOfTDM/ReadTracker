package com.sniperoftdm.booktracker.domain

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Calculates initial delays for daily reminder scheduling without platform dependencies.
 */
object NextReminderCalculator {

    /** Computes the duration until the next target reminder time, handling day boundaries and DST shifts. */
    fun delayUntilNext(
        now: ZonedDateTime,
        hour: Int,
        minute: Int,
        zone: ZoneId = now.zone
    ): Duration {
        val targetToday = now.withZoneSameInstant(zone)
            .toLocalDate()
            .atTime(hour, minute)
            .atZone(zone)

        val target = if (targetToday.isAfter(now)) {
            targetToday
        } else {
            now.withZoneSameInstant(zone)
                .toLocalDate()
                .plusDays(1)
                .atTime(hour, minute)
                .atZone(zone)
        }

        return Duration.between(now, target)
    }
}
