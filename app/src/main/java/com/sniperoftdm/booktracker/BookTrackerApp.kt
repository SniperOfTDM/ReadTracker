package com.sniperoftdm.booktracker

import android.app.Application
import com.sniperoftdm.booktracker.worker.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookTrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        CoroutineScope(Dispatchers.Default).launch {
            ReminderScheduler.schedule(this@BookTrackerApp)
        }
    }
}
