package com.sniperoftdm.booktracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sniperoftdm.booktracker.domain.ValidationReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(name = "active_timer")

class TimerRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val bookRepository: BookRepository
) : TimerRepository {

    private object PreferencesKeys {
        val BOOK_ID = longPreferencesKey("active_timer_book_id")
        val STARTED_AT = longPreferencesKey("active_timer_started_at")
    }

    override fun observeActive(): Flow<ActiveTimer?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val bookId = preferences[PreferencesKeys.BOOK_ID]
            val startedAt = preferences[PreferencesKeys.STARTED_AT]
            if (bookId != null && startedAt != null) {
                ActiveTimer(bookId = bookId, startedAtMillis = startedAt)
            } else {
                null
            }
        }

    override suspend fun start(bookId: Long, startedAtMillis: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BOOK_ID] = bookId
            preferences[PreferencesKeys.STARTED_AT] = startedAtMillis
        }
    }

    override suspend fun cancel() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.BOOK_ID)
            preferences.remove(PreferencesKeys.STARTED_AT)
        }
    }

    override suspend fun stopAndLog(endPage: Int): BookResult<Long> {
        val active = observeActive().first()
            ?: return BookResult.ValidationError(ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS)

        val now = System.currentTimeMillis()
        val durationSeconds = ((now - active.startedAtMillis) / 1000).toInt().coerceAtLeast(0)

        val result = bookRepository.logCheckpoint(
            bookId = active.bookId,
            endPage = endPage,
            timestamp = now,
            durationSeconds = durationSeconds
        )

        if (result is BookResult.Success) {
            cancel()
        }
        return result
    }
}
