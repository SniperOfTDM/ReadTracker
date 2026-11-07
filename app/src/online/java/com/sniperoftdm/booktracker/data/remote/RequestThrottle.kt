package com.sniperoftdm.booktracker.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe rate limiter enforcing minimum time spacing between HTTP requests.
 */
class RequestThrottle(
    val minSpacingMs: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private var lastRequestTime = 0L

    /** Enforce spacing by delaying if elapsed time since last call is under minSpacingMs. */
    suspend fun acquire() {
        mutex.withLock {
            val now = clock()
            val elapsed = now - lastRequestTime
            if (lastRequestTime > 0L && elapsed in 0 until minSpacingMs) {
                val waitTime = minSpacingMs - elapsed
                delay(waitTime)
            }
            lastRequestTime = clock()
        }
    }
}
