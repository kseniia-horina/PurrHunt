package app.purrhunt

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.purrhunt.data.reminders.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PurrHuntApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}
