package com.sniperoftdm.booktracker.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persisted state of an active reading session timer.
 */
data class ActiveTimer(
    val bookId: Long,
    val startedAtMillis: Long
)

interface TimerRepository {

    /** Observes the currently running reading timer, or null if inactive. */
    fun observeActive(): Flow<ActiveTimer?>

    /** Starts or restarts the reading timer for a given book. */
    suspend fun start(bookId: Long, startedAtMillis: Long = System.currentTimeMillis())

    /** Cancels the currently running timer without saving a checkpoint. */
    suspend fun cancel()

    /** Computes elapsed duration, validates and records the checkpoint, stopping the timer on success. */
    suspend fun stopAndLog(endPage: Int): BookResult<Long>
}
