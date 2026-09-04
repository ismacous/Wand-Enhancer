package com.ismael.daybyday.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ismael.daybyday.R
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.ui.MainActivity
import java.time.LocalDate

/** Rappel du soir : ne sonne que si la journee n'est pas encore notee. */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext.dayByDayApp
        if (!app.prefs.reminderEnabled) return Result.success()

        val today = app.repository.dayOnce(LocalDate.now())
        if (today?.colorKey != null) return Result.success()

        val name = app.prefs.firstName.trim()
        val title = if (name.isEmpty()) "Et cette journée ?" else "Alors $name, cette journée ?"

        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Prends 30 secondes pour lui donner une couleur.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "rappel_quotidien"
        const val NOTIFICATION_ID = 1001
    }
}
