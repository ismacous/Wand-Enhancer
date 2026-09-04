package com.ismael.daybyday.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lecture des pas depuis Health Connect, le standard Android ou Samsung Health
 * ecrit ses donnees. Tout se passe en local sur le telephone : l'application
 * n'a pas la permission Internet, elle ne peut rien envoyer nulle part.
 */
object HealthConnectSource {

    val permissions: Set<String> = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    fun isAvailable(context: Context): Boolean =
        runCatching { HealthConnectClient.getSdkStatus(context) }
            .getOrNull() == HealthConnectClient.SDK_AVAILABLE

    private fun client(context: Context): HealthConnectClient? =
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()

    suspend fun hasPermission(context: Context): Boolean {
        if (!isAvailable(context)) return false
        val client = client(context) ?: return false
        return runCatching {
            client.permissionController.getGrantedPermissions().containsAll(permissions)
        }.getOrDefault(false)
    }

    /** Nombre de pas de la journee, ou null si la lecture n'est pas possible. */
    suspend fun stepsFor(context: Context, date: LocalDate): Int? {
        if (!hasPermission(context)) return null
        val client = client(context) ?: return null
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val result: AggregationResult = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
        }.getOrNull() ?: return null
        return result[StepsRecord.COUNT_TOTAL]?.toInt()
    }
}
