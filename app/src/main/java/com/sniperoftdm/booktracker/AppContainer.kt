package com.sniperoftdm.booktracker

import android.content.Context
import com.sniperoftdm.booktracker.data.backup.BackupRepository
import com.sniperoftdm.booktracker.data.backup.BackupRepositoryImpl
import com.sniperoftdm.booktracker.data.local.BookTrackerDatabase
import com.sniperoftdm.booktracker.data.local.CoverStorage
import com.sniperoftdm.booktracker.data.repository.BookLookupRepository
import com.sniperoftdm.booktracker.data.repository.BookRepository
import com.sniperoftdm.booktracker.data.repository.BookRepositoryImpl
import com.sniperoftdm.booktracker.data.repository.SettingsRepository
import com.sniperoftdm.booktracker.data.repository.SettingsRepositoryImpl
import com.sniperoftdm.booktracker.data.repository.TimerRepository
import com.sniperoftdm.booktracker.data.repository.TimerRepositoryImpl
import com.sniperoftdm.booktracker.data.repository.settingsDataStore
import com.sniperoftdm.booktracker.data.repository.timerDataStore
import com.sniperoftdm.booktracker.domain.SessionValidator
import com.sniperoftdm.booktracker.domain.StatsCalculator
import com.sniperoftdm.booktracker.domain.StatsConfig
import com.sniperoftdm.booktracker.flavor.FlavorDependencies
import com.sniperoftdm.booktracker.ui.widget.BookTrackerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    val database: BookTrackerDatabase =
        BookTrackerDatabase.getInstance(context)

    val coverStorage: CoverStorage =
        CoverStorage(context)

    val bookLookupRepository: BookLookupRepository =
        FlavorDependencies.provideBookLookupRepository(context)

    val sessionValidator: SessionValidator = SessionValidator()

    val settingsRepository: SettingsRepository =
        SettingsRepositoryImpl(context.settingsDataStore)

    val statsConfig: StatsConfig = StatsConfig()

    val statsCalculator: StatsCalculator = StatsCalculator(statsConfig)

    private val notifyWidgetUpdate: () -> Unit = {
        applicationScope.launch {
            BookTrackerWidget.updateAll(context)
        }
    }

    val repository: BookRepository =
        BookRepositoryImpl(
            bookDao = database.bookDao(),
            sessionDao = database.sessionDao(),
            noteDao = database.noteDao(),
            coverStorage = coverStorage,
            validator = sessionValidator,
            onDataChanged = notifyWidgetUpdate
        )

    val timerRepository: TimerRepository =
        TimerRepositoryImpl(
            dataStore = context.timerDataStore,
            bookRepository = repository
        )

    val backupRepository: BackupRepository =
        BackupRepositoryImpl(
            database = database,
            settingsRepository = settingsRepository,
            coverStorage = coverStorage,
            validator = sessionValidator,
            onDataChanged = notifyWidgetUpdate
        )
}
