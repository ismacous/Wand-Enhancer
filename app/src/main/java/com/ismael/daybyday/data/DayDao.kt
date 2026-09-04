package com.ismael.daybyday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {

    // --- Journees ---------------------------------------------------------

    @Query("SELECT * FROM day_entries WHERE epochDay = :epochDay")
    fun observeDay(epochDay: Long): Flow<DayEntry?>

    @Query("SELECT * FROM day_entries WHERE epochDay = :epochDay")
    suspend fun dayOnce(epochDay: Long): DayEntry?

    @Query("SELECT * FROM day_entries WHERE epochDay BETWEEN :start AND :end")
    fun observeRange(start: Long, end: Long): Flow<List<DayEntry>>

    @Query("SELECT * FROM day_entries ORDER BY epochDay")
    fun observeAll(): Flow<List<DayEntry>>

    @Query("SELECT * FROM day_entries ORDER BY epochDay")
    suspend fun allDays(): List<DayEntry>

    @Upsert
    suspend fun upsertDay(entry: DayEntry)

    @Query("DELETE FROM day_entries WHERE epochDay = :epochDay")
    suspend fun deleteDay(epochDay: Long)

    @Query(
        "SELECT * FROM day_entries " +
            "WHERE title LIKE '%' || :text || '%' OR note LIKE '%' || :text || '%' " +
            "ORDER BY epochDay DESC LIMIT 300"
    )
    fun search(text: String): Flow<List<DayEntry>>

    @Query("SELECT epochDay AS epochDay, weightKg AS weightKg FROM day_entries WHERE weightKg IS NOT NULL ORDER BY epochDay")
    fun observeWeights(): Flow<List<WeightPoint>>

    // --- Etiquettes -------------------------------------------------------

    @Query("SELECT * FROM tags ORDER BY sortOrder, name")
    fun observeTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY sortOrder, name")
    suspend fun allTags(): List<Tag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Query("DELETE FROM day_tags WHERE tagId = :tagId")
    suspend fun deleteTagLinks(tagId: Long)

    @Query(
        "SELECT tags.* FROM tags INNER JOIN day_tags ON tags.id = day_tags.tagId " +
            "WHERE day_tags.epochDay = :epochDay ORDER BY tags.sortOrder, tags.name"
    )
    fun observeTagsForDay(epochDay: Long): Flow<List<Tag>>

    @Query("SELECT * FROM day_tags")
    fun observeAllDayTags(): Flow<List<DayTagCrossRef>>

    @Query("SELECT * FROM day_tags")
    suspend fun allDayTags(): List<DayTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkTag(crossRef: DayTagCrossRef)

    @Query("DELETE FROM day_tags WHERE epochDay = :epochDay AND tagId = :tagId")
    suspend fun unlinkTag(epochDay: Long, tagId: Long)

    @Query("DELETE FROM day_tags WHERE epochDay = :epochDay")
    suspend fun unlinkAllTagsOfDay(epochDay: Long)

    @Query("SELECT COUNT(*) FROM day_tags WHERE epochDay = :epochDay")
    suspend fun tagCountForDay(epochDay: Long): Int

    // --- Argent -----------------------------------------------------------

    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, id DESC")
    fun observeAllMoney(): Flow<List<MoneyEntry>>

    @Query("SELECT * FROM transactions WHERE epochDay BETWEEN :start AND :end ORDER BY epochDay DESC, id DESC")
    fun observeMoneyBetween(start: Long, end: Long): Flow<List<MoneyEntry>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions")
    fun observeMoneyBalance(): Flow<Long>

    @Query("SELECT * FROM transactions ORDER BY epochDay, id")
    suspend fun allMoney(): List<MoneyEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMoney(entry: MoneyEntry): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteMoney(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllMoney()

    // --- Medias -----------------------------------------------------------

    @Query("SELECT * FROM media_items WHERE epochDay = :epochDay ORDER BY addedAt, id")
    fun observeMediaForDay(epochDay: Long): Flow<List<MediaItem>>

    @Query(
        "SELECT epochDay AS epochDay, COUNT(*) AS count FROM media_items " +
            "WHERE epochDay BETWEEN :start AND :end GROUP BY epochDay"
    )
    fun observeMediaCounts(start: Long, end: Long): Flow<List<DayMediaCount>>

    @Query("SELECT * FROM media_items WHERE epochDay = :epochDay ORDER BY addedAt, id")
    suspend fun mediaForDay(epochDay: Long): List<MediaItem>

    @Query("SELECT * FROM media_items ORDER BY epochDay, addedAt, id")
    suspend fun allMedia(): List<MediaItem>

    @Insert
    suspend fun insertMedia(item: MediaItem): Long

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMedia(id: Long)

    // --- Remise a zero ----------------------------------------------------

    @Query("DELETE FROM media_items")
    suspend fun deleteAllMedia()

    @Query("DELETE FROM day_entries")
    suspend fun deleteAllDays()

    @Query("DELETE FROM day_tags")
    suspend fun deleteAllDayTags()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
