package com.ismael.daybyday.health

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

/**
 * Temps reel passe sur le telephone, lu depuis les evenements d'usage
 * d'Android. On additionne des intervalles qui ne se chevauchent pas : quand
 * plusieurs applications se relaient, la periode ne compte qu'une seule fois.
 *
 * Additionner le "temps au premier plan" de chaque application, comme le fait
 * queryAndAggregateUsageStats, donne des totaux absurdes (16 h dans une
 * journee) parce que les periodes des applications se recouvrent.
 */
object ScreenTimeSource {

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Minutes d'utilisation reelle du telephone ce jour-la, ou null sans autorisation. */
    fun minutesFor(context: Context, date: LocalDate): Int? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = minOf(
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            System.currentTimeMillis(),
        )
        if (dayEnd <= dayStart) return null

        val events = runCatching { manager.queryEvents(dayStart, dayEnd) }.getOrNull() ?: return null

        var total = 0L
        var openedAt = 0L
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                // Une application passe au premier plan : le telephone est utilise.
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    if (openedAt == 0L) openedAt = event.timeStamp

                // Retour en arriere-plan ou ecran eteint : on ferme l'intervalle.
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN,
                -> if (openedAt != 0L) {
                    total += (event.timeStamp - openedAt).coerceAtLeast(0L)
                    openedAt = 0L
                }
            }
        }

        // Session encore ouverte a la fin de la periode observee.
        if (openedAt != 0L) total += (dayEnd - openedAt).coerceAtLeast(0L)

        // Filet de securite : jamais plus que le temps ecoule dans la journee.
        val elapsed = dayEnd - dayStart
        val minutes = (minOf(total, elapsed) / 60_000L).toInt()
        return if (minutes <= 0) null else minutes
    }
}
