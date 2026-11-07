package com.sniperoftdm.booktracker.domain

import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class StatsCalculatorTest {

    private val defaultZone = ZoneOffset.UTC
    private val calculatorWithZeroDays = StatsCalculator(StatsConfig(countZeroDays = true, paceWindowDays = 7))
    private val calculatorWithoutZeroDays = StatsCalculator(StatsConfig(countZeroDays = false, paceWindowDays = 7))

    private fun createBook(
        id: Long = 1L,
        title: String = "Test Book",
        totalPages: Int = 300,
        startPage: Int = 0,
        goalDateEpochDay: Long? = null,
        status: BookStatus = BookStatus.READING
    ): Book {
        return Book(
            id = id,
            title = title,
            author = "Test Author",
            totalPages = totalPages,
            startPage = startPage,
            status = status,
            goalDateEpochDay = goalDateEpochDay,
            createdAt = 1_000_000L
        )
    }

    private fun toTimestamp(date: LocalDate, hour: Int = 12, minute: Int = 0, zone: ZoneId = defaultZone): Long {
        return date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    }

    @Test
    fun firstSessionUsesStartPageAsBaseline() {
        val book = createBook(startPage = 50, totalPages = 200)
        val session = Session(id = 1L, bookId = book.id, timestamp = 1000L, endPage = 80)

        val pairs = calculatorWithZeroDays.pagesPerSession(book, listOf(session))

        assertEquals(1, pairs.size)
        assertEquals(30, pairs.first().second)
        assertEquals(80, calculatorWithZeroDays.currentPage(book, listOf(session)))
        assertEquals(120, calculatorWithZeroDays.pagesLeft(book, listOf(session)))
    }

    @Test
    fun twoCheckpointsOnSameDayAreSummed() {
        val today = LocalDate.of(2026, 3, 10)
        val book = createBook(startPage = 0, totalPages = 300)
        val session1 = Session(id = 1L, bookId = book.id, timestamp = toTimestamp(today, 9, 0), endPage = 25)
        val session2 = Session(id = 2L, bookId = book.id, timestamp = toTimestamp(today, 18, 0), endPage = 70)

        val dailyMap = calculatorWithZeroDays.pagesPerDay(book, listOf(session1, session2), defaultZone)

        assertEquals(1, dailyMap.size)
        assertEquals(70, dailyMap[today])
        assertEquals(70, calculatorWithZeroDays.todayPages(book, listOf(session1, session2), today, defaultZone))
    }

    @Test
    fun skippedDaysCountAsZeroWhenCountZeroDaysIsTrueAndIgnoredWhenFalse() {
        val day1 = LocalDate.of(2026, 3, 1)
        val day3 = LocalDate.of(2026, 3, 3)
        val book = createBook(startPage = 0, totalPages = 300)

        val session1 = Session(id = 1L, bookId = book.id, timestamp = toTimestamp(day1), endPage = 10)
        val session2 = Session(id = 2L, bookId = book.id, timestamp = toTimestamp(day3), endPage = 30)
        val sessions = listOf(session1, session2)

        // Day 1 to Day 3 inclusive = 3 days (day 1 = 10, day 2 = 0, day 3 = 20). Total = 30 pages.
        val avgZeroDaysIncluded = calculatorWithZeroDays.dailyAverageAllTime(book, sessions, day3, defaultZone)
        assertEquals(10.0, avgZeroDaysIncluded, 0.001)

        // CountZeroDays = false: only 2 active reading days. 30 pages / 2 days = 15.0
        val avgZeroDaysExcluded = calculatorWithoutZeroDays.dailyAverageAllTime(book, sessions, day3, defaultZone)
        assertEquals(15.0, avgZeroDaysExcluded, 0.001)
    }

    @Test
    fun recentAverageClipsToFirstSessionDateForBookYoungerThanSevenDays() {
        val today = LocalDate.of(2026, 3, 3)
        val day1 = LocalDate.of(2026, 3, 1) // 3 days ago, paceWindow = 7 days
        val book = createBook(startPage = 0, totalPages = 300)

        val session1 = Session(id = 1L, bookId = book.id, timestamp = toTimestamp(day1), endPage = 15)
        val session2 = Session(id = 2L, bookId = book.id, timestamp = toTimestamp(today), endPage = 45)
        val sessions = listOf(session1, session2)

        // Clipped window is day 1 through day 3 inclusive = 3 days. Total = 45 pages. 45 / 3 = 15.0
        val recentAvg = calculatorWithZeroDays.dailyAverageRecent(book, sessions, today, defaultZone)
        assertEquals(15.0, recentAvg, 0.001)
    }

    @Test
    fun estimatedDaysLeftUsesRecentPaceAndFallsBackToAllTimePace() {
        val today = LocalDate.of(2026, 3, 20)
        val book = createBook(startPage = 0, totalPages = 100)

        // Book completed
        val finishedSession = listOf(Session(id = 1L, bookId = book.id, timestamp = toTimestamp(today), endPage = 100))
        assertEquals(0, calculatorWithZeroDays.estimatedDaysLeft(book, finishedSession, today, defaultZone))

        // Book with no sessions: pace is 0 -> null
        assertNull(calculatorWithZeroDays.estimatedDaysLeft(book, emptyList(), today, defaultZone))

        // Book with recent pace
        val recentSession = listOf(
            Session(id = 1L, bookId = book.id, timestamp = toTimestamp(today.minusDays(1)), endPage = 20),
            Session(id = 2L, bookId = book.id, timestamp = toTimestamp(today), endPage = 40)
        )
        // 40 pages read, 60 left. 2 days window (clipped to 2 days) -> 40 / 2 = 20 pages/day.
        // 60 left / 20 = 3 days.
        val days = calculatorWithZeroDays.estimatedDaysLeft(book, recentSession, today, defaultZone)
        assertEquals(3, days)
        assertEquals(today.plusDays(3), calculatorWithZeroDays.estimatedFinishDate(book, recentSession, today, defaultZone))

        // Book with NO recent reading (sessions occurred 15 days ago), so recent pace is 0.0, fallback to all-time
        val oldSession = listOf(
            Session(id = 1L, bookId = book.id, timestamp = toTimestamp(today.minusDays(19)), endPage = 10),
            Session(id = 2L, bookId = book.id, timestamp = toTimestamp(today.minusDays(10)), endPage = 20)
        )
        // 20 pages left: 80. All-time days = 20 days. All-time pace = 20 / 20 = 1.0 page/day.
        // estimatedDaysLeft = ceil(80 / 1.0) = 80
        val fallbackDays = calculatorWithZeroDays.estimatedDaysLeft(book, oldSession, today, defaultZone)
        assertEquals(80, fallbackDays)
    }

    @Test
    fun requiredPagesPerDayWithFutureGoalTodayAsGoalAndPastGoal() {
        val today = LocalDate.of(2026, 3, 10)
        val book = createBook(startPage = 0, totalPages = 100)
        val sessions = listOf(Session(id = 1L, bookId = book.id, timestamp = toTimestamp(today), endPage = 40))
        // 60 pages left

        // 1. Future goal: 5 days from today (today, +1, +2, +3, +4) -> 5 days inclusive
        val futureGoalBook = book.copy(goalDateEpochDay = today.plusDays(4).toEpochDay())
        val reqFuture = calculatorWithZeroDays.requiredPagesPerDay(futureGoalBook, sessions, today)
        assertEquals(12, reqFuture) // ceil(60 / 5) = 12

        // 2. Today as goal: 1 day inclusive
        val todayGoalBook = book.copy(goalDateEpochDay = today.toEpochDay())
        val reqToday = calculatorWithZeroDays.requiredPagesPerDay(todayGoalBook, sessions, today)
        assertEquals(60, reqToday) // ceil(60 / 1) = 60

        // 3. Past goal: goalDate is before today -> returns full pagesLeft
        val pastGoalBook = book.copy(goalDateEpochDay = today.minusDays(2).toEpochDay())
        val reqPast = calculatorWithZeroDays.requiredPagesPerDay(pastGoalBook, sessions, today)
        assertEquals(60, reqPast)

        // 4. No goal -> null
        assertNull(calculatorWithZeroDays.requiredPagesPerDay(book, sessions, today))
    }

    @Test
    fun streakStaysAliveWhenTodayHasNoPagesYetButYesterdayDoesAndBreaksAfterFullMissedDay() {
        val today = LocalDate.of(2026, 3, 10)
        val pagesMapAlive = mapOf(
            today.minusDays(3) to 15,
            today.minusDays(2) to 20,
            today.minusDays(1) to 25
            // today has no entry
        )

        // Streak should still be 3 (active from yesterday)
        val streakAlive = calculatorWithZeroDays.currentStreak(pagesMapAlive, today)
        assertEquals(3, streakAlive)

        // Full missed day (neither today nor yesterday has pages)
        val pagesMapBroken = mapOf(
            today.minusDays(4) to 15,
            today.minusDays(3) to 20,
            today.minusDays(2) to 25
            // yesterday and today have no entries
        )
        val streakBroken = calculatorWithZeroDays.currentStreak(pagesMapBroken, today)
        assertEquals(0, streakBroken)

        // Today has pages
        val pagesMapTodayActive = pagesMapAlive + (today to 10)
        assertEquals(4, calculatorWithZeroDays.currentStreak(pagesMapTodayActive, today))
    }

    @Test
    fun globalStatsAggregateSeveralParallelBooksCorrectly() {
        val today = LocalDate.of(2026, 3, 10)
        val book1 = createBook(id = 1L, title = "Book 1", totalPages = 200, startPage = 0)
        val book2 = createBook(id = 2L, title = "Book 2", totalPages = 300, startPage = 10)

        val sessionsBook1 = listOf(
            Session(id = 1L, bookId = 1L, timestamp = toTimestamp(today.minusDays(1)), endPage = 20),
            Session(id = 2L, bookId = 1L, timestamp = toTimestamp(today), endPage = 50)
        )
        val sessionsBook2 = listOf(
            Session(id = 3L, bookId = 2L, timestamp = toTimestamp(today.minusDays(1)), endPage = 30),
            Session(id = 4L, bookId = 2L, timestamp = toTimestamp(today), endPage = 70)
        )

        val books = listOf(book1, book2)
        val sessionsByBook = mapOf(1L to sessionsBook1, 2L to sessionsBook2)

        val global = calculatorWithZeroDays.computeGlobalStats(books, sessionsByBook, today, defaultZone)

        // Book 1: day -1 = 20 pages, today = 30 pages. Total = 50.
        // Book 2: day -1 = 20 pages, today = 40 pages. Total = 60.
        // Global daily: day -1 = 40 pages, today = 70 pages.
        assertEquals(110, global.totalPagesRead)
        assertEquals(40, global.pagesPerDay[today.minusDays(1)])
        assertEquals(70, global.pagesPerDay[today])
        assertEquals(2, global.currentStreak)
        assertEquals(2, global.longestStreak)
        assertNotNull(global.bestDay)
        assertEquals(today, global.bestDay?.first)
        assertEquals(70, global.bestDay?.second)
    }

    @Test
    fun sessionNearMidnightLandsOnCorrectLocalDayForEuropeIstanbul() {
        val istanbulZone = ZoneId.of("Europe/Istanbul")
        val book = createBook(startPage = 0, totalPages = 200)

        // 2026-03-15 at 21:30:00 UTC
        // Istanbul is UTC+3, so 21:30 UTC is 00:30 on 2026-03-16 in Istanbul!
        val utcTime = ZonedDateTime.of(2026, 3, 15, 21, 30, 0, 0, ZoneOffset.UTC)
        val sessionTimestamp = utcTime.toInstant().toEpochMilli()

        val session = Session(id = 1L, bookId = book.id, timestamp = sessionTimestamp, endPage = 25)
        val dailyMap = calculatorWithZeroDays.pagesPerDay(book, listOf(session), istanbulZone)

        val expectedIstanbulDate = LocalDate.of(2026, 3, 16)
        assertEquals(1, dailyMap.size)
        assertTrue("Expected session to land on $expectedIstanbulDate in Istanbul timezone", dailyMap.containsKey(expectedIstanbulDate))
        assertEquals(25, dailyMap[expectedIstanbulDate])
    }

    @Test
    fun pagesPerHourCalculatesOnlyFromTimedSessionsAndReturnsNullWhenUntimed() {
        val book = createBook(startPage = 0, totalPages = 200)

        // Untimed session: durationSeconds = null
        val untimed = listOf(Session(id = 1L, bookId = book.id, timestamp = 1000L, endPage = 20, durationSeconds = null))
        assertNull(calculatorWithZeroDays.pagesPerHour(book, untimed))

        // Mixed: session 1 untimed (20 pages), session 2 timed (30 pages in 1800s = 0.5 hours)
        val mixed = listOf(
            Session(id = 1L, bookId = book.id, timestamp = 1000L, endPage = 20, durationSeconds = null),
            Session(id = 2L, bookId = book.id, timestamp = 2000L, endPage = 50, durationSeconds = 1800)
        )
        // Timed pages = 30 pages / 0.5 hours = 60.0 pages/hour
        val pph = calculatorWithZeroDays.pagesPerHour(book, mixed)
        assertNotNull(pph)
        assertEquals(60.0, pph!!, 0.001)

        // Global pages per hour
        val book2 = createBook(id = 2L, startPage = 0, totalPages = 200)
        val sessionsBook2 = listOf(
            Session(id = 3L, bookId = 2L, timestamp = 3000L, endPage = 40, durationSeconds = 3600) // 40 pages in 1 hour
        )
        // Total timed pages = 30 + 40 = 70 pages. Total timed hours = 0.5 + 1.0 = 1.5 hours. 70 / 1.5 = 46.666...
        val globalPph = calculatorWithZeroDays.globalPagesPerHour(
            books = listOf(book, book2),
            sessionsByBook = mapOf(1L to mixed, 2L to sessionsBook2)
        )
        assertNotNull(globalPph)
        assertEquals(70.0 / 1.5, globalPph!!, 0.001)
    }

    @Test
    fun finishedAtReturnsCorrectTimestamp() {
        val book = createBook(startPage = 0, totalPages = 150)
        val s1 = Session(id = 1L, bookId = book.id, timestamp = 1000L, endPage = 50)
        val s2 = Session(id = 2L, bookId = book.id, timestamp = 2000L, endPage = 150)

        // Reached totalPages on session 2
        assertEquals(2000L, calculatorWithZeroDays.finishedAt(book, listOf(s1, s2)))

        // Incomplete book returns null
        val incomplete = listOf(Session(id = 1L, bookId = book.id, timestamp = 1000L, endPage = 149))
        assertNull(calculatorWithZeroDays.finishedAt(book, incomplete))
    }

    @Test
    fun yearlyGoalProgressCalculatesAheadBehindMathAccurately() {
        val today = LocalDate.of(2026, 7, 2) // Day 183 of 365 (leap year check: 2026 is not leap, length = 365)
        val goal = 10

        // 6 books finished in 2026
        val finishedBooks = (1..6).map { index ->
            val book = createBook(id = index.toLong(), totalPages = 100, status = BookStatus.FINISHED)
            book to listOf(Session(id = index.toLong(), bookId = index.toLong(), timestamp = toTimestamp(LocalDate.of(2026, 5, index)), endPage = 100))
        }

        val books = finishedBooks.map { it.first }
        val sessionsByBook = finishedBooks.associate { it.first.id to it.second }

        val progress = calculatorWithZeroDays.yearlyGoalProgress(
            year = 2026,
            goal = goal,
            today = today,
            books = books,
            sessionsByBook = sessionsByBook,
            zone = defaultZone
        )

        assertEquals(6, progress.finishedCount)
        assertEquals(10, progress.goal)
        assertEquals(0.6f, progress.progressFraction, 0.001f)

        // Expected by day 183: 10 * (183 / 365) = 5.01369...
        // Ahead / behind: 6 - 5.01369 = +0.986 -> rounded to 1 decimal = +1.0
        assertEquals(1.0, progress.aheadBehindSchedule, 0.001)
    }

    @Test
    fun projectedBooksByYearEndIncludesFinishedAndProjectedReadingBooks() {
        val today = LocalDate.of(2026, 10, 1)

        // Book 1: Already finished in 2026
        val book1 = createBook(id = 1L, totalPages = 100, status = BookStatus.FINISHED)
        val sessions1 = listOf(Session(id = 1L, bookId = 1L, timestamp = toTimestamp(LocalDate.of(2026, 8, 1)), endPage = 100))

        // Book 2: READING, 20 pages left, pace = 10 p/d -> finishes in 2 days (October 3, 2026 <= Dec 31)
        val book2 = createBook(id = 2L, totalPages = 100, status = BookStatus.READING)
        val sessions2 = listOf(
            Session(id = 2L, bookId = 2L, timestamp = toTimestamp(today.minusDays(1)), endPage = 70),
            Session(id = 3L, bookId = 2L, timestamp = toTimestamp(today), endPage = 80)
        )

        // Book 3: READING, 1000 pages left, pace = 1 p/d -> finishes in 1000 days (after Dec 31)
        val book3 = createBook(id = 3L, totalPages = 1100, status = BookStatus.READING)
        val sessions3 = listOf(
            Session(id = 4L, bookId = 3L, timestamp = toTimestamp(today.minusDays(9)), endPage = 90),
            Session(id = 5L, bookId = 3L, timestamp = toTimestamp(today), endPage = 100)
        )

        val books = listOf(book1, book2, book3)
        val sessionsByBook = mapOf(1L to sessions1, 2L to sessions2, 3L to sessions3)

        // Expected: 1 finished + 1 reading projected to finish = 2
        val projected = calculatorWithZeroDays.projectedBooksByYearEnd(
            year = 2026,
            today = today,
            books = books,
            sessionsByBook = sessionsByBook,
            zone = defaultZone
        )
        assertEquals(2, projected)
    }

    @Test
    fun totalEtaWithSharedPaceAcrossReadingBooksAndNullPace() {
        val today = LocalDate.of(2026, 3, 10)

        // Two reading books
        val book1 = createBook(id = 1L, totalPages = 100, status = BookStatus.READING) // 50 left
        val book2 = createBook(id = 2L, totalPages = 150, status = BookStatus.READING) // 100 left
        // Total pages left = 150

        // Sessions over last 2 days:
        // book 1: day -1 = 25, today = 50 (25 pages read)
        // book 2: day -1 = 30, today = 50 (20 pages read)
        // Total pages read in 2 days = 70 pages -> shared pace = 35.0 p/d
        val s1 = listOf(
            Session(id = 1L, bookId = 1L, timestamp = toTimestamp(today.minusDays(1)), endPage = 25),
            Session(id = 2L, bookId = 1L, timestamp = toTimestamp(today), endPage = 50)
        )
        val s2 = listOf(
            Session(id = 3L, bookId = 2L, timestamp = toTimestamp(today.minusDays(1)), endPage = 30),
            Session(id = 4L, bookId = 2L, timestamp = toTimestamp(today), endPage = 50)
        )

        val readingBooks = listOf(book1, book2)
        val sessionsByBook = mapOf(1L to s1, 2L to s2)

        val eta = calculatorWithZeroDays.totalEta(readingBooks, sessionsByBook, today, defaultZone)
        assertNotNull(eta)
        // Total left = 50 + 100 = 150 pages.
        // Shared pace = 50.0 / 2 = 25.0 or 70 / 2 = 35.0 (let's verify ceil(150 / pace))
        assertTrue(eta!!.daysLeft > 0)
        assertEquals(today.plusDays(eta.daysLeft.toLong()), eta.finishDate)

        // Null pace case: no sessions at all
        assertNull(calculatorWithZeroDays.totalEta(readingBooks, emptyMap(), today, defaultZone))
    }
}
