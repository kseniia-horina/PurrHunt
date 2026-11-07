package app.purrhunt.data.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.purrhunt.R
import app.purrhunt.domain.GameSettings
import app.purrhunt.domain.ReminderTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(settings: GameSettings) {
        val wm = WorkManager.getInstance(context)
        if (!settings.remindersEnabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        val next = ReminderTime.next(now, settings.reminderDays, settings.reminderHour, settings.reminderMinute)
        if (next == null) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = Duration.between(now, next).toMillis()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val CHANNEL_ID = "play_reminders"
        private const val WORK_NAME = "play_reminder"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_desc) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
