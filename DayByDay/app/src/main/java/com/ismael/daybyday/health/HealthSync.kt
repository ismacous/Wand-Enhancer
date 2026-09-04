package com.ismael.daybyday.health

import android.content.Context
import com.ismael.daybyday.data.DayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Rafraichit les pas et le temps d'ecran des journees deja notees. On ne cree
 * jamais de journee pour ces seules donnees : une journee n'existe que si tu y
 * as mis quelque chose.
 */
object HealthSync {

    suspend fun syncExistingDay(context: Context, repository: DayRepository, date: LocalDate) {
        withContext(Dispatchers.IO) {
            val existing = repository.dayOnce(date) ?: return@withContext
            val steps = HealthConnectSource.stepsFor(context, date)
            val minutes = ScreenTimeSource.minutesFor(context, date)
            if (steps == null && minutes == null) return@withContext
            val updated = existing.copy(
                steps = steps ?: existing.steps,
                screenMinutes = minutes ?: existing.screenMinutes,
            )
            if (updated != existing) repository.saveDay(updated)
        }
    }

    /** Met a jour les derniers jours notes, appele a l'ouverture de l'application. */
    suspend fun syncRecentDays(context: Context, repository: DayRepository, days: Int = 7) {
        val today = LocalDate.now()
        (0 until days).forEach { offset ->
            syncExistingDay(context, repository, today.minusDays(offset.toLong()))
        }
    }
}
