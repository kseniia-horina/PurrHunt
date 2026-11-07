package app.purrhunt.data.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.purrhunt.MainActivity
import app.purrhunt.R
import app.purrhunt.data.CatRepository
import app.purrhunt.data.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
    private val cats: CatRepository,
    private val scheduler: ReminderScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val current = settings.current()
        if (current.remindersEnabled && canNotify()) {
            val catName = settings.appState.first().currentCatId?.let { cats.get(it)?.name }
            val text = catName?.let { applicationContext.getString(R.string.notif_text, it) }
                ?: applicationContext.getString(R.string.notif_text_generic)
            val intent = PendingIntent.getActivity(
                applicationContext, 0,
                Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(applicationContext.getString(R.string.notif_title))
                .setContentText(text)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
        scheduler.schedule(current)
        return Result.success()
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val NOTIFICATION_ID = 1001
    }
}
