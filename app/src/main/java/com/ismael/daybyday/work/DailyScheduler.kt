package com.ismael.daybyday.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ismael.daybyday.data.Prefs
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Planifie les deux taches quotidiennes locales : le rappel du soir et la
 * sauvegarde automatique. Tout passe par WorkManager, donc ca survit aux
 * redemarrages du telephone sans permission supplementaire.
 */
object DailyScheduler {

    const val REMINDER_WORK = "daybyday-rappel-quotidien"
    const val BACKUP_WORK = "daybyday-sauvegarde-quotidienne"

    /** Millisecondes jusqu'a la prochaine occurrence de [hour]:[minute]. */
    fun initialDelayMillis(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var target = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }

    fun scheduleReminder(context: Context, prefs: Prefs) {
        val manager = WorkManager.getInstance(context.applicationContext)
        if (!prefs.reminderEnabled) {
            manager.cancelUniqueWork(REMINDER_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(
                initialDelayMillis(prefs.reminderHour, prefs.reminderMinute),
                TimeUnit.MILLISECONDS,
            )
            .build()
        manager.enqueueUniquePeriodicWork(
            REMINDER_WORK,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    fun scheduleAutoBackup(context: Context, prefs: Prefs) {
        val manager = WorkManager.getInstance(context.applicationContext)
        if (!prefs.autoBackupEnabled || prefs.autoBackupFolder == null) {
            manager.cancelUniqueWork(BACKUP_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(
                initialDelayMillis(prefs.autoBackupHour, prefs.autoBackupMinute),
                TimeUnit.MILLISECONDS,
            )
            .build()
        manager.enqueueUniquePeriodicWork(
            BACKUP_WORK,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    fun rescheduleAll(context: Context, prefs: Prefs) {
        scheduleReminder(context, prefs)
        scheduleAutoBackup(context, prefs)
    }
}
