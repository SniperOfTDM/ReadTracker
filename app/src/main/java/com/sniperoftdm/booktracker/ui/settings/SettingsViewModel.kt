package com.sniperoftdm.booktracker.ui.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.backup.BackupRepository
import com.sniperoftdm.booktracker.data.backup.ImportMode
import com.sniperoftdm.booktracker.data.backup.ImportReport
import com.sniperoftdm.booktracker.data.backup.ImportResult
import com.sniperoftdm.booktracker.data.local.BookTrackerDatabase
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.SettingsRepository
import com.sniperoftdm.booktracker.data.repository.ThemeMode
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.ui.log.QuickLogActivity
import com.sniperoftdm.booktracker.worker.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

sealed interface BackupOperationState {
    data object Idle : BackupOperationState
    data object ExportSuccess : BackupOperationState
    data class ExportError(val message: String) : BackupOperationState
    data class ImportSuccess(val books: Int, val sessions: Int, val notes: Int) : BackupOperationState
    data class ImportValidationFailed(val report: ImportReport) : BackupOperationState
    data class ImportError(val message: String) : BackupOperationState
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository? = null,
    private val database: BookTrackerDatabase? = null,
    private val bookRepository: BookRepository? = null
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
        )

    private val _backupState = MutableStateFlow<BackupOperationState>(BackupOperationState.Idle)
    val backupState: StateFlow<BackupOperationState> = _backupState.asStateFlow()

    fun resetBackupState() {
        _backupState.value = BackupOperationState.Idle
    }

    fun setCountZeroDays(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCountZeroDays(enabled)
        }
    }

    fun setPaceWindowDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setPaceWindowDays(days)
        }
    }

    fun setYearlyGoalBooks(goal: Int) {
        viewModelScope.launch {
            settingsRepository.setYearlyGoalBooks(goal)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseDynamicColor(enabled)
        }
    }

    fun exportBackup(outputStream: OutputStream) {
        val repo = backupRepository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.export(outputStream)
            _backupState.value = if (result.isSuccess) {
                BackupOperationState.ExportSuccess
            } else {
                BackupOperationState.ExportError(result.exceptionOrNull()?.message ?: "Export failed")
            }
        }
    }

    fun importBackup(inputStream: InputStream) {
        val repo = backupRepository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repo.import(inputStream, ImportMode.REPLACE_ALL)) {
                is ImportResult.Success -> {
                    _backupState.value = BackupOperationState.ImportSuccess(
                        books = result.booksImported,
                        sessions = result.sessionsImported,
                        notes = result.notesImported
                    )
                }
                is ImportResult.ValidationError -> {
                    _backupState.value = BackupOperationState.ImportValidationFailed(result.report)
                }
                is ImportResult.Error -> {
                    _backupState.value = BackupOperationState.ImportError(result.message)
                }
            }
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            database?.clearAllTables()
            settingsRepository.updateSettings(UserSettings())
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun sendTestNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, QuickLogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Test Reminder: Keep your daily reading momentum going!")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
