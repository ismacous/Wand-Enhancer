package com.ismael.daybyday.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

class DayRepository(context: Context) {

    private val dao = AppDatabase.get(context).dayDao()
    val media = MediaFiles(context)

    fun observeDay(date: LocalDate): Flow<DayEntry?> = dao.observeDay(date.toEpochDay())

    fun observeMediaForDay(date: LocalDate): Flow<List<MediaItem>> =
        dao.observeMediaForDay(date.toEpochDay())

    fun observeDaysBetween(start: LocalDate, end: LocalDate): Flow<Map<Long, DayEntry>> =
        dao.observeRange(start.toEpochDay(), end.toEpochDay())
            .map { list -> list.associateBy { it.epochDay } }

    fun observeMediaCounts(start: LocalDate, end: LocalDate): Flow<Map<Long, Int>> =
        dao.observeMediaCounts(start.toEpochDay(), end.toEpochDay())
            .map { list -> list.associate { it.epochDay to it.count } }

    fun observeAllDays(): Flow<List<DayEntry>> = dao.observeAll()

    suspend fun allDays(): List<DayEntry> = dao.allDays()

    suspend fun allMedia(): List<MediaItem> = dao.allMedia()

    suspend fun mediaForDay(date: LocalDate): List<MediaItem> = dao.mediaForDay(date.toEpochDay())

    /**
     * Enregistre le contenu d'une journee. Une journee totalement vide et sans
     * media est supprimee pour ne pas polluer les statistiques.
     */
    suspend fun saveDay(date: LocalDate, colorKey: Int?, title: String, note: String) {
        val epochDay = date.toEpochDay()
        val entry = DayEntry(
            epochDay = epochDay,
            colorKey = colorKey,
            title = title.trim(),
            note = note,
            updatedAt = System.currentTimeMillis(),
        )
        if (entry.isEmpty && dao.mediaForDay(epochDay).isEmpty()) {
            dao.deleteDay(epochDay)
        } else {
            dao.upsertDay(entry)
        }
    }

    suspend fun addMedia(date: LocalDate, uri: Uri): Boolean {
        val item = media.importFrom(uri, date.toEpochDay()) ?: return false
        dao.insertMedia(item)
        // Une journee qui contient un media doit exister en base.
        if (dao.dayOnce(date.toEpochDay()) == null) {
            dao.upsertDay(DayEntry(epochDay = date.toEpochDay()))
        }
        return true
    }

    suspend fun deleteMedia(item: MediaItem) {
        dao.deleteMedia(item.id)
        media.delete(item.relativePath)
        val epochDay = item.epochDay
        val remaining = dao.mediaForDay(epochDay)
        val entry = dao.dayOnce(epochDay)
        if (remaining.isEmpty() && entry != null && entry.isEmpty) {
            dao.deleteDay(epochDay)
        }
    }

    suspend fun clearEverything() {
        dao.deleteAllMedia()
        dao.deleteAllDays()
        media.deleteAll()
    }

    suspend fun replaceAll(days: List<DayEntry>, mediaItems: List<MediaItem>) {
        dao.deleteAllMedia()
        dao.deleteAllDays()
        days.forEach { dao.upsertDay(it) }
        mediaItems.forEach { dao.insertMedia(it.copy(id = 0)) }
    }

    fun monthRange(month: YearMonth): Pair<LocalDate, LocalDate> =
        month.atDay(1) to month.atEndOfMonth()
}
