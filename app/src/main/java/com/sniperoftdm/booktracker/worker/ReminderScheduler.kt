package com.sniperoftdm.booktracker.worker

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sniperoftdm.booktracker.data.repository.settingsDataStore
import com.sniperoftdm.booktracker.domain.NextReminderCalculator
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    const val WORK_NAME = "daily_reading_reminder_work"
    const val CHANNEL_ID = "reading_reminder"

    suspend fun schedule(context: Context) {
        val preferences = context.settingsDataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()

        val enabled = preferences[androidx.datastore.preferences.core.booleanPreferencesKey("reminder_enabled")] ?: false
        if (!enabled) {
            cancel(context)
            return
        }

        val hour = preferences[androidx.datastore.preferences.core.intPreferencesKey("reminder_hour")] ?: 21
        val minute = preferences[androidx.datastore.preferences.core.intPreferencesKey("reminder_minute")] ?: 0

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val delay = NextReminderCalculator.delayUntilNext(
            now = now,
            hour = hour,
            minute = minute,
            zone = now.zone
        )

        val workRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .addTag("reading_reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
