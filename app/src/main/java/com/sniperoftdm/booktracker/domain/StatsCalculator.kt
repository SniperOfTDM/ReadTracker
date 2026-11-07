package com.sniperoftdm.booktracker.domain

import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.model.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.round

/**
 * Calculates reading statistics and pace projections without platform dependencies.
 */
class StatsCalculator(private val config: StatsConfig = StatsConfig()) {

    /** Computes the pages read in each individual session in chronological order. */
    fun pagesPerSession(book: Book, sessions: List<Session>): List<Pair<Session, Int>> {
        val sorted = sessions.sortedWith(compareBy({ it.timestamp }, { it.id }))
        var previousEndPage = book.startPage
        return sorted.map { session ->
            val pagesRead = session.endPage - previousEndPage
            previousEndPage = session.endPage
            session to pagesRead
        }
    }

    /** Aggregates pages read per calendar day for a single book. */
    fun pagesPerDay(book: Book, sessions: List<Session>, zone: ZoneId): Map<LocalDate, Int> {
        val pairs = pagesPerSession(book, sessions)
        val result = mutableMapOf<LocalDate, Int>()
        for ((session, pages) in pairs) {
            val date = Instant.ofEpochMilli(session.timestamp).atZone(zone).toLocalDate()
            result[date] = (result[date] ?: 0) + pages
        }
        return result
    }

    /** Returns the current furthest page reached in the book. */
    fun currentPage(book: Book, sessions: List<Session>): Int {
        val sorted = sessions.sortedWith(compareBy({ it.timestamp }, { it.id }))
        return sorted.lastOrNull()?.endPage ?: book.startPage
    }

    /** Returns the remaining number of pages until the book is completed. */
    fun pagesLeft(book: Book, sessions: List<Session>): Int {
        return (book.totalPages - currentPage(book, sessions)).coerceAtLeast(0)
    }

    /** Calculates the reading progress fraction clamped between 0.0 and 1.0. */
    fun progressPercent(book: Book, sessions: List<Session>): Float {
        if (book.totalPages <= 0) return 0f
        return (currentPage(book, sessions).toFloat() / book.totalPages.toFloat()).coerceIn(0f, 1f)
    }

    /** Returns the total pages read on the specified target day for a single book. */
    fun todayPages(book: Book, sessions: List<Session>, today: LocalDate, zone: ZoneId): Int {
        return pagesPerDay(book, sessions, zone)[today] ?: 0
    }

    /** Calculates the all-time daily reading average from the first session through today. */
    fun dailyAverageAllTime(book: Book, sessions: List<Session>, today: LocalDate, zone: ZoneId): Double {
        if (sessions.isEmpty()) return 0.0
        val sorted = sessions.sortedWith(compareBy({ it.timestamp }, { it.id }))
        val firstDate = Instant.ofEpochMilli(sorted.first().timestamp).atZone(zone).toLocalDate()
        val dailyMap = pagesPerDay(book, sessions, zone)
        val totalRead = dailyMap.values.sum()
        if (totalRead <= 0) return 0.0

        val daysCounted = if (config.countZeroDays) {
            maxOf(1L, ChronoUnit.DAYS.between(firstDate, today) + 1)
        } else {
            val activeDays = dailyMap.count { it.value > 0 }
            maxOf(1L, activeDays.toLong())
        }

        return totalRead.toDouble() / daysCounted.toDouble()
    }

    /** Calculates the recent daily reading average across the configured pace window. */
    fun dailyAverageRecent(book: Book, sessions: List<Session>, today: LocalDate, zone: ZoneId): Double {
        if (sessions.isEmpty()) return 0.0
        val sorted = sessions.sortedWith(compareBy({ it.timestamp }, { it.id }))
        val firstDate = Instant.ofEpochMilli(sorted.first().timestamp).atZone(zone).toLocalDate()

        val theoreticalWindowStart = today.minusDays((config.paceWindowDays - 1).toLong())
        val effectiveStart = if (firstDate.isAfter(theoreticalWindowStart)) firstDate else theoreticalWindowStart
        if (effectiveStart.isAfter(today)) return 0.0

        val dailyMap = pagesPerDay(book, sessions, zone)
        val pagesInWindow = dailyMap.entries
            .filter { (date, _) -> !date.isBefore(effectiveStart) && !date.isAfter(today) }
            .sumOf { it.value }

        val daysCounted = if (config.countZeroDays) {
            maxOf(1L, ChronoUnit.DAYS.between(effectiveStart, today) + 1)
        } else {
            val activeDays = dailyMap.entries.count { (date, pages) ->
                !date.isBefore(effectiveStart) && !date.isAfter(today) && pages > 0
            }
            if (activeDays == 0) return 0.0
            activeDays.toLong()
        }

        return pagesInWindow.toDouble() / daysCounted.toDouble()
    }

    /** Estimates how many days remain until the book is completed based on current pace. */
    fun estimatedDaysLeft(book: Book, sessions: List<Session>, today: LocalDate, zone: ZoneId): Int? {
        val remaining = pagesLeft(book, sessions)
        if (remaining == 0) return 0

        val recentPace = dailyAverageRecent(book, sessions, today, zone)
        val pace = if (recentPace > 0.0) {
            recentPace
        } else {
            dailyAverageAllTime(book, sessions, today, zone)
        }

        if (pace <= 0.0) return null
        return ceil(remaining.toDouble() / pace).toInt()
    }

    /** Projects the calendar date on which the book will be finished. */
    fun estimatedFinishDate(book: Book, sessions: List<Session>, today: LocalDate, zone: ZoneId): LocalDate? {
        val days = estimatedDaysLeft(book, sessions, today, zone) ?: return null
        return today.plusDays(days.toLong())
    }

    /** Computes the daily pages required to hit the user's target goal date. */
    fun requiredPagesPerDay(book: Book, sessions: List<Session>, today: LocalDate): Int? {
        val goalDay = book.goalDateEpochDay ?: return null
        val goalDate = LocalDate.ofEpochDay(goalDay)
        val remaining = pagesLeft(book, sessions)
        if (remaining == 0) return 0
        if (goalDate.isBefore(today)) return remaining

        val daysUntilGoal = maxOf(1L, ChronoUnit.DAYS.between(today, goalDate) + 1)
        return ceil(remaining.toDouble() / daysUntilGoal.toDouble()).toInt()
    }

    /** Determines whether the current reading pace meets the target finish date requirements. */
    fun isOnTrack(book: Book, sessions: List<Session>, today: LocalDate, zone: ZoneId): Boolean? {
        val required = requiredPagesPerDay(book, sessions, today) ?: return null
        val recentPace = dailyAverageRecent(book, sessions, today, zone)
        val pace = if (recentPace > 0.0) {
            recentPace
        } else {
            dailyAverageAllTime(book, sessions, today, zone)
        }
        return pace >= required.toDouble()
    }

    /** Aggregates pages read across all books by local calendar date. */
    fun globalPagesPerDay(
        books: List<Book>,
        sessionsByBook: Map<Long, List<Session>>,
        zone: ZoneId
    ): Map<LocalDate, Int> {
        val aggregate = mutableMapOf<LocalDate, Int>()
        for (book in books) {
            val sessions = sessionsByBook[book.id] ?: emptyList()
            val bookDaily = pagesPerDay(book, sessions, zone)
            for ((date, pages) in bookDaily) {
                aggregate[date] = (aggregate[date] ?: 0) + pages
            }
        }
        return aggregate
    }

    /** Computes the active daily reading streak ending today or preserved from yesterday. */
    fun currentStreak(allPagesPerDay: Map<LocalDate, Int>, today: LocalDate): Int {
        val todayPages = allPagesPerDay[today] ?: 0
        val anchorDate = if (todayPages > 0) {
            today
        } else {
            val yesterdayPages = allPagesPerDay[today.minusDays(1)] ?: 0
            if (yesterdayPages > 0) today.minusDays(1) else null
        } ?: return 0

        var streak = 0
        var checkDate = anchorDate
        while ((allPagesPerDay[checkDate] ?: 0) > 0) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        return streak
    }

    /** Calculates the historical maximum consecutive active reading days. */
    fun longestStreak(allPagesPerDay: Map<LocalDate, Int>, today: LocalDate): Int {
        val activeDates = allPagesPerDay.entries
            .filter { (date, pages) -> !date.isAfter(today) && pages > 0 }
            .map { it.key }
            .sorted()

        if (activeDates.isEmpty()) return 0

        var maxRun = 0
        var currentRun = 0
        var previousDate: LocalDate? = null

        for (date in activeDates) {
            if (previousDate == null || date == previousDate.plusDays(1)) {
                currentRun++
            } else {
                currentRun = 1
            }
            previousDate = date
            if (currentRun > maxRun) {
                maxRun = currentRun
            }
        }
        return maxRun
    }

    /** Identifies the single day with the highest number of pages read. */
    fun bestDay(allPagesPerDay: Map<LocalDate, Int>): Pair<LocalDate, Int>? {
        val activeDays = allPagesPerDay.filter { it.value > 0 }
        if (activeDays.isEmpty()) return null
        val bestEntry = activeDays.maxByOrNull { it.value } ?: return null
        return bestEntry.key to bestEntry.value
    }

    /** Calculates the cumulative number of pages read across all books. */
    fun totalPagesRead(books: List<Book>, sessionsByBook: Map<Long, List<Session>>): Int {
        var total = 0
        for (book in books) {
            val sessions = sessionsByBook[book.id] ?: emptyList()
            total += pagesPerSession(book, sessions).sumOf { it.second }
        }
        return total
    }

    /** Computes the total duration in seconds spent reading across timed sessions. */
    fun totalReadingSeconds(sessions: List<Session>): Long {
        return sessions.sumOf { (it.durationSeconds ?: 0).toLong() }
    }

    /** Calculates reading speed in pages per hour based only on timed sessions. */
    fun pagesPerHour(book: Book, sessions: List<Session>): Double? {
        val pairs = pagesPerSession(book, sessions)
        val timedPairs = pairs.filter { it.first.durationSeconds != null && it.first.durationSeconds!! > 0 }
        if (timedPairs.isEmpty()) return null

        val totalTimedPages = timedPairs.sumOf { it.second }
        val totalTimedSeconds = timedPairs.sumOf { it.first.durationSeconds!! }
        if (totalTimedSeconds == 0) return null

        val hours = totalTimedSeconds.toDouble() / 3600.0
        return totalTimedPages.toDouble() / hours
    }

    /** Calculates the global reading speed in pages per hour across all books from timed sessions. */
    fun globalPagesPerHour(books: List<Book>, sessionsByBook: Map<Long, List<Session>>): Double? {
        var totalTimedPages = 0
        var totalTimedSeconds = 0L

        for (book in books) {
            val sessions = sessionsByBook[book.id] ?: emptyList()
            val pairs = pagesPerSession(book, sessions)
            val timedPairs = pairs.filter { it.first.durationSeconds != null && it.first.durationSeconds!! > 0 }
            totalTimedPages += timedPairs.sumOf { it.second }
            totalTimedSeconds += timedPairs.sumOf { it.first.durationSeconds!!.toLong() }
        }

        if (totalTimedSeconds == 0L) return null
        val hours = totalTimedSeconds.toDouble() / 3600.0
        return totalTimedPages.toDouble() / hours
    }

    /** Returns the timestamp when the book reached its total page count, or null if incomplete. */
    fun finishedAt(book: Book, sessions: List<Session>): Long? {
        val sorted = sessions.sortedWith(compareBy({ it.timestamp }, { it.id }))
        return sorted.firstOrNull { it.endPage == book.totalPages }?.timestamp
    }

    /** Returns the list of books completed within a specific calendar year. */
    fun booksFinishedInYear(
        year: Int,
        books: List<Book>,
        sessionsByBook: Map<Long, List<Session>>,
        zone: ZoneId
    ): List<Book> {
        return books.filter { book ->
            val sessions = sessionsByBook[book.id] ?: emptyList()
            val finishedTimestamp = finishedAt(book, sessions) ?: return@filter false
            val finishedDate = Instant.ofEpochMilli(finishedTimestamp).atZone(zone).toLocalDate()
            finishedDate.year == year
        }
    }

    /** Computes annual reading goal progress and current pace variance ahead or behind schedule. */
    fun yearlyGoalProgress(
        year: Int,
        goal: Int,
        today: LocalDate,
        books: List<Book>,
        sessionsByBook: Map<Long, List<Session>>,
        zone: ZoneId
    ): YearlyGoalProgress {
        val finishedCount = booksFinishedInYear(year, books, sessionsByBook, zone).size
        val fraction = if (goal <= 0) 0f else (finishedCount.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
        val daysInYear = today.lengthOfYear()
        val dayOfYear = today.dayOfYear.toDouble()
        val expected = if (goal <= 0) 0.0 else goal.toDouble() * (dayOfYear / daysInYear.toDouble())
        val diff = finishedCount.toDouble() - expected
        val aheadBehind = round(diff * 10.0) / 10.0

        return YearlyGoalProgress(
            finishedCount = finishedCount,
            goal = goal,
            progressFraction = fraction,
            aheadBehindSchedule = aheadBehind
        )
    }

    /** Projects total books that will be completed by year end assuming current pace continues. */
    fun projectedBooksByYearEnd(
        year: Int,
        today: LocalDate,
        books: List<Book>,
        sessionsByBook: Map<Long, List<Session>>,
        zone: ZoneId
    ): Int {
        val finishedSoFar = booksFinishedInYear(year, books, sessionsByBook, zone).size
        val dec31 = LocalDate.of(year, 12, 31)

        val readingProjected = books.count { book ->
            if (book.status != BookStatus.READING) return@count false
            val sessions = sessionsByBook[book.id] ?: emptyList()
            val finishDate = estimatedFinishDate(book, sessions, today, zone) ?: return@count false
            !finishDate.isAfter(dec31)
        }

        return finishedSoFar + readingProjected
    }

    /** Projects completion date across reading books assuming one shared global pace divided among them. */
    fun totalEta(
        readingBooks: List<Book>,
        sessionsByBook: Map<Long, List<Session>>,
        today: LocalDate,
        zone: ZoneId
    ): TotalEta? {
        val allSessions = sessionsByBook.values.flatten()
        if (allSessions.isEmpty()) return null

        val sortedSessions = allSessions.sortedWith(compareBy({ it.timestamp }, { it.id }))
        val firstDate = Instant.ofEpochMilli(sortedSessions.first().timestamp).atZone(zone).toLocalDate()

        val theoreticalWindowStart = today.minusDays((config.paceWindowDays - 1).toLong())
        val effectiveStart = if (firstDate.isAfter(theoreticalWindowStart)) firstDate else theoreticalWindowStart

        val globalDaily = globalPagesPerDay(readingBooks, sessionsByBook, zone)

        val pagesInWindow = globalDaily.entries
            .filter { (date, _) -> !date.isBefore(effectiveStart) && !date.isAfter(today) }
            .sumOf { it.value }

        val windowDaysCounted = if (config.countZeroDays) {
            maxOf(1L, ChronoUnit.DAYS.between(effectiveStart, today) + 1)
        } else {
            val active = globalDaily.entries.count { (date, p) -> !date.isBefore(effectiveStart) && !date.isAfter(today) && p > 0 }
            if (active == 0) 0L else active.toLong()
        }

        val recentPace = if (windowDaysCounted > 0) pagesInWindow.toDouble() / windowDaysCounted.toDouble() else 0.0

        val allTimeDaysCounted = if (config.countZeroDays) {
            maxOf(1L, ChronoUnit.DAYS.between(firstDate, today) + 1)
        } else {
            val active = globalDaily.count { it.value > 0 }
            if (active == 0) 0L else active.toLong()
        }
        val totalPages = globalDaily.values.sum()
        val allTimePace = if (allTimeDaysCounted > 0) totalPages.toDouble() / allTimeDaysCounted.toDouble() else 0.0

        val sharedPace = if (recentPace > 0.0) recentPace else allTimePace
        if (sharedPace <= 0.0) return null

        val totalPagesLeft = readingBooks.sumOf { pagesLeft(it, sessionsByBook[it.id] ?: emptyList()) }
        if (totalPagesLeft == 0) {
            return TotalEta(daysLeft = 0, finishDate = today, sharedPace = sharedPace)
        }

        val days = ceil(totalPagesLeft.toDouble() / sharedPace).toInt()
        val finishDate = today.plusDays(days.toLong())
        return TotalEta(daysLeft = days, finishDate = finishDate, sharedPace = sharedPace)
    }

    /** Bundles all computed metrics for a single book into an immutable snapshot. */
    fun computeBookStats(book: Book, sessions: List<Session>, today: LocalDate, zone: ZoneId): BookStats {
        return BookStats(
            currentPage = currentPage(book, sessions),
            pagesLeft = pagesLeft(book, sessions),
            progressPercent = progressPercent(book, sessions),
            todayPages = todayPages(book, sessions, today, zone),
            dailyAverageAllTime = dailyAverageAllTime(book, sessions, today, zone),
            dailyAverageRecent = dailyAverageRecent(book, sessions, today, zone),
            estimatedDaysLeft = estimatedDaysLeft(book, sessions, today, zone),
            estimatedFinishDate = estimatedFinishDate(book, sessions, today, zone),
            requiredPagesPerDay = requiredPagesPerDay(book, sessions, today),
            isOnTrack = isOnTrack(book, sessions, today, zone),
            pagesPerDay = pagesPerDay(book, sessions, zone),
            sessionsWithPages = pagesPerSession(book, sessions),
            pagesPerHour = pagesPerHour(book, sessions),
            totalReadingSeconds = totalReadingSeconds(sessions)
        )
    }

    /** Bundles all global reading metrics across all books into an immutable snapshot. */
    fun computeGlobalStats(
        books: List<Book>,
        sessionsByBook: Map<Long, List<Session>>,
        today: LocalDate,
        zone: ZoneId
    ): GlobalStats {
        val daily = globalPagesPerDay(books, sessionsByBook, zone)
        return GlobalStats(
            currentStreak = currentStreak(daily, today),
            longestStreak = longestStreak(daily, today),
            bestDay = bestDay(daily),
            totalPagesRead = totalPagesRead(books, sessionsByBook),
            pagesPerDay = daily,
            pagesPerHour = globalPagesPerHour(books, sessionsByBook)
        )
    }
}
