package com.ismael.daybyday.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DayRepository(context: Context) {

    private val dao = AppDatabase.get(context).dayDao()
    val media = MediaFiles(context)

    // --- Journees ---------------------------------------------------------

    fun observeDay(date: LocalDate): Flow<DayEntry?> = dao.observeDay(date.toEpochDay())

    fun observeDaysBetween(start: LocalDate, end: LocalDate): Flow<Map<Long, DayEntry>> =
        dao.observeRange(start.toEpochDay(), end.toEpochDay())
            .map { list -> list.associateBy { it.epochDay } }

    fun observeAllDays(): Flow<List<DayEntry>> = dao.observeAll()

    fun search(text: String): Flow<List<DayEntry>> = dao.search(text)

    fun observeWeights(): Flow<List<WeightPoint>> = dao.observeWeights()

    suspend fun allDays(): List<DayEntry> = dao.allDays()

    suspend fun dayOnce(date: LocalDate): DayEntry? = dao.dayOnce(date.toEpochDay())

    /**
     * Enregistre le contenu d'une journee. Une journee totalement vide, sans
     * etiquette ni media, est supprimee pour ne pas polluer les statistiques.
     */
    suspend fun saveDay(entry: DayEntry) {
        val epochDay = entry.epochDay
        val hasExtras = dao.tagCountForDay(epochDay) > 0 || dao.mediaForDay(epochDay).isNotEmpty()
        if (entry.isEmpty && !hasExtras) {
            dao.deleteDay(epochDay)
        } else {
            dao.upsertDay(entry.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /** Cree la ligne du jour si elle n'existe pas encore (media, etiquette...). */
    private suspend fun ensureDayExists(epochDay: Long) {
        if (dao.dayOnce(epochDay) == null) dao.upsertDay(DayEntry(epochDay = epochDay))
    }

    // --- Etiquettes -------------------------------------------------------

    fun observeTags(): Flow<List<Tag>> = dao.observeTags()

    fun observeTagsForDay(date: LocalDate): Flow<List<Tag>> =
        dao.observeTagsForDay(date.toEpochDay())

    fun observeAllDayTags(): Flow<List<DayTagCrossRef>> = dao.observeAllDayTags()

    suspend fun allTags(): List<Tag> = dao.allTags()

    suspend fun allDayTags(): List<DayTagCrossRef> = dao.allDayTags()

    suspend fun createTag(name: String, emoji: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val order = (dao.allTags().maxOfOrNull { it.sortOrder } ?: 0) + 1
        dao.insertTag(Tag(name = trimmed, emoji = emoji.trim(), sortOrder = order))
    }

    suspend fun deleteTag(tag: Tag) {
        dao.deleteTagLinks(tag.id)
        dao.deleteTag(tag.id)
    }

    suspend fun toggleTag(date: LocalDate, tag: Tag, selected: Boolean) {
        val epochDay = date.toEpochDay()
        if (selected) {
            ensureDayExists(epochDay)
            dao.linkTag(DayTagCrossRef(epochDay = epochDay, tagId = tag.id))
        } else {
            dao.unlinkTag(epochDay, tag.id)
            cleanUpIfEmpty(epochDay)
        }
    }

    private suspend fun cleanUpIfEmpty(epochDay: Long) {
        val entry = dao.dayOnce(epochDay) ?: return
        val hasExtras = dao.tagCountForDay(epochDay) > 0 || dao.mediaForDay(epochDay).isNotEmpty()
        if (entry.isEmpty && !hasExtras) dao.deleteDay(epochDay)
    }

    // --- Medias -----------------------------------------------------------

    fun observeMediaForDay(date: LocalDate): Flow<List<MediaItem>> =
        dao.observeMediaForDay(date.toEpochDay())

    fun observeMediaCounts(start: LocalDate, end: LocalDate): Flow<Map<Long, Int>> =
        dao.observeMediaCounts(start.toEpochDay(), end.toEpochDay())
            .map { list -> list.associate { it.epochDay to it.count } }

    suspend fun allMedia(): List<MediaItem> = dao.allMedia()

    suspend fun addMedia(date: LocalDate, uri: Uri): Boolean {
        val item = media.importFrom(uri, date.toEpochDay()) ?: return false
        dao.insertMedia(item)
        ensureDayExists(date.toEpochDay())
        return true
    }

    suspend fun deleteMedia(item: MediaItem) {
        dao.deleteMedia(item.id)
        media.delete(item.relativePath)
        cleanUpIfEmpty(item.epochDay)
    }

    // --- Sauvegarde / remise a zero ---------------------------------------

    suspend fun clearEverything() {
        dao.deleteAllMedia()
        dao.deleteAllDayTags()
        dao.deleteAllDays()
        media.deleteAll()
    }

    suspend fun replaceAll(
        days: List<DayEntry>,
        mediaItems: List<MediaItem>,
        tags: List<Tag>,
        links: List<DayTagCrossRef>,
    ) {
        dao.deleteAllMedia()
        dao.deleteAllDayTags()
        dao.deleteAllDays()
        if (tags.isNotEmpty()) {
            dao.deleteAllTags()
            tags.forEach { dao.insertTag(it) }
        }
        days.forEach { dao.upsertDay(it) }
        mediaItems.forEach { dao.insertMedia(it.copy(id = 0)) }
        links.forEach { dao.linkTag(it) }
    }
}
