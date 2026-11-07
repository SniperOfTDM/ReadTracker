package com.sniperoftdm.booktracker.ui.debug

import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.BookResult
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

object SeedData {

    suspend fun clearAll(repository: BookRepository) {
        val books = repository.observeBooks().first()
        for (book in books) {
            repository.deleteBook(book)
        }
    }

    suspend fun seed(repository: BookRepository, zone: ZoneId = ZoneId.systemDefault()) {
        val today = LocalDate.now(zone)

        // Book 1: Atomic Habits (Active reading with a future goal)
        val book1Id = (repository.addBook(
            title = "Atomic Habits",
            author = "James Clear",
            totalPages = 320,
            startPage = 0,
            goalDateEpochDay = today.plusDays(10).toEpochDay()
        ) as? BookResult.Success)?.data ?: return

        // Book 2: Clean Architecture (Mid-way reading without a goal)
        val book2Id = (repository.addBook(
            title = "Clean Architecture",
            author = "Robert C. Martin",
            totalPages = 400,
            startPage = 20,
            goalDateEpochDay = null
        ) as? BookResult.Success)?.data ?: return

        // Book 3: Dune (Long fantasy with ambitious goal)
        val book3Id = (repository.addBook(
            title = "Dune",
            author = "Frank Herbert",
            totalPages = 600,
            startPage = 0,
            goalDateEpochDay = today.plusDays(30).toEpochDay()
        ) as? BookResult.Success)?.data ?: return

        // Seed 21 days of sessions for Book 1
        var page1 = 0
        for (daysAgo in 20 downTo 1) {
            if (daysAgo in listOf(17, 12, 6)) continue // Skipped days

            val date = today.minusDays(daysAgo.toLong())
            val ts1 = date.atTime(10, 30).atZone(zone).toInstant().toEpochMilli()
            page1 += 12
            repository.logCheckpoint(bookId = book1Id, endPage = page1, timestamp = ts1)

            // Two checkpoints on days 10 and 3
            if (daysAgo == 10 || daysAgo == 3) {
                val ts2 = date.atTime(21, 15).atZone(zone).toInstant().toEpochMilli()
                page1 += 10
                repository.logCheckpoint(bookId = book1Id, endPage = page1, timestamp = ts2)
            }
        }
        // Today checkpoint for Book 1
        val todayTs1 = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        page1 += 15
        repository.logCheckpoint(bookId = book1Id, endPage = page1, timestamp = todayTs1)

        // Seed sessions for Book 2
        var page2 = 20
        for (daysAgo in 18 downTo 2) {
            if (daysAgo in listOf(15, 14, 9, 8, 4)) continue // Skipped days
            val date = today.minusDays(daysAgo.toLong())
            val ts = date.atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
            page2 += 16
            repository.logCheckpoint(bookId = book2Id, endPage = page2, timestamp = ts)
        }

        // Seed sessions for Book 3
        var page3 = 0
        for (daysAgo in 15 downTo 1) {
            if (daysAgo in listOf(13, 10, 7, 5)) continue
            val date = today.minusDays(daysAgo.toLong())
            val ts = date.atTime(22, 0).atZone(zone).toInstant().toEpochMilli()
            page3 += 25
            repository.logCheckpoint(bookId = book3Id, endPage = page3, timestamp = ts)
        }
    }
}
