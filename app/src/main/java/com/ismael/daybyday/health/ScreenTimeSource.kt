package com.ismael.daybyday.health

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

/**
 * Temps passe sur les applications, lu depuis les statistiques d'usage
 * d'Android. Necessite l'autorisation speciale "Acces aux donnees
 * d'utilisation", accordee a la main dans les reglages du telephone.
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

    /** Minutes passees sur les applications ce jour-la, ou null sans autorisation. */
    fun minutesFor(context: Context, date: LocalDate): Int? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            System.currentTimeMillis(),
        )
        if (end <= start) return null

        val stats = runCatching { manager.queryAndAggregateUsageStats(start, end) }
            .getOrNull() ?: return null
        if (stats.isEmpty()) return null
        val totalMillis = stats.values.sumOf { it.totalTimeInForeground }
        return (totalMillis / 60_000L).toInt()
    }
}
