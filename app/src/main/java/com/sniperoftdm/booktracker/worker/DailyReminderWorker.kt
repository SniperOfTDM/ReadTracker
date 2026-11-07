package com.sniperoftdm.booktracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.local.BookTrackerDatabase
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.domain.StatsCalculator
import com.sniperoftdm.booktracker.ui.log.QuickLogActivity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = BookTrackerDatabase.getInstance(applicationContext)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val books = database.bookDao().observeAll().first()
        val sessions = database.sessionDao().observeAll().first()

        val calculator = StatsCalculator()
        val sessionsByBook = sessions.groupBy { it.bookId }
        val dailyMap = calculator.globalPagesPerDay(books, sessionsByBook, zone)
        val pagesToday = dailyMap[today] ?: 0

        // Only notify if no pages have been logged today
        if (pagesToday <= 0) {
            val streak = calculator.currentStreak(dailyMap, today)
            val readingBook = books.firstOrNull { it.status == BookStatus.READING }

            val message = when {
                streak > 0 -> applicationContext.getString(R.string.reminder_streak_text, streak)
                readingBook != null -> applicationContext.getString(R.string.reminder_book_text, readingBook.title)
                else -> applicationContext.getString(R.string.reminder_default_text)
            }

            sendNotification(message)
        }

        // Reschedule next reminder
        ReminderScheduler.schedule(applicationContext)

        return Result.success()
    }

    private fun sendNotification(message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, QuickLogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }
}
