package com.sniperoftdm.booktracker.ui

import com.sniperoftdm.booktracker.data.model.Session
import com.sniperoftdm.booktracker.ui.components.ChartAxisHelper
import com.sniperoftdm.booktracker.ui.components.HeatmapLevelCalculator
import com.sniperoftdm.booktracker.ui.detail.BookDetailHelper
import com.sniperoftdm.booktracker.ui.detail.DayListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StageBHelpersTest {

    @Test
    fun chartAxisHelper_roundsUpCorrectly() {
        assertEquals(10, ChartAxisHelper.roundUpMax(0))
        assertEquals(10, ChartAxisHelper.roundUpMax(7))
        assertEquals(25, ChartAxisHelper.roundUpMax(18))
        assertEquals(50, ChartAxisHelper.roundUpMax(45))
        assertEquals(100, ChartAxisHelper.roundUpMax(88))
        assertEquals(200, ChartAxisHelper.roundUpMax(140))
    }

    @Test
    fun heatmapLevelCalculator_calculatesQuartilesAndIntensity() {
        val today = LocalDate.of(2026, 11, 7)
        val testData = mapOf(
            today.minusDays(5) to 10,
            today.minusDays(4) to 20,
            today.minusDays(3) to 30,
            today.minusDays(2) to 40,
            today.minusDays(1) to 50
        )

        val thresholds = HeatmapLevelCalculator.calculateThresholds(testData)
        assertTrue(thresholds.q1 > 0)
        assertTrue(thresholds.q2 >= thresholds.q1)
        assertTrue(thresholds.q3 >= thresholds.q2)

        assertEquals(0, HeatmapLevelCalculator.intensityLevel(0, thresholds))
        assertEquals(1, HeatmapLevelCalculator.intensityLevel(thresholds.q1, thresholds))
        assertEquals(4, HeatmapLevelCalculator.intensityLevel(thresholds.q3 + 10, thresholds))
    }

    @Test
    fun bookDetailHelper_insertsZeroReadingGapsCorrectly() {
        val zone = ZoneId.of("UTC")
        val oct1 = LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val oct4 = LocalDate.of(2026, 10, 4).atStartOfDay(zone).toInstant().toEpochMilli()

        val sessions = listOf(
            Session(id = 1, bookId = 1, timestamp = oct1, endPage = 30) to 30,
            Session(id = 2, bookId = 1, timestamp = oct4, endPage = 60) to 30
        )

        val dayItems = BookDetailHelper.buildDayList(sessions, zone)

        // Returned in descending order:
        // Index 0: ReadingDay (Day 4 - Oct 4)
        // Index 1: ZeroReadingGap (2 days: Oct 2 & Oct 3)
        // Index 2: ReadingDay (Day 1 - Oct 1)
        assertEquals(3, dayItems.size)

        val latestDay = dayItems[0] as DayListItem.ReadingDay
        assertEquals(4L, latestDay.dayNumber)
        assertEquals(LocalDate.of(2026, 10, 4), latestDay.date)
        assertEquals(30, latestDay.totalPagesRead)

        val gap = dayItems[1] as DayListItem.ZeroReadingGap
        assertEquals(2, gap.daysCount)
        assertEquals(LocalDate.of(2026, 10, 2), gap.startDate)
        assertEquals(LocalDate.of(2026, 10, 3), gap.endDate)

        val firstDay = dayItems[2] as DayListItem.ReadingDay
        assertEquals(1L, firstDay.dayNumber)
        assertEquals(LocalDate.of(2026, 10, 1), firstDay.date)
    }
}
