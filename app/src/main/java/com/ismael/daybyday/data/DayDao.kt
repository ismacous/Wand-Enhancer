package com.ismael.daybyday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {

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

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun mediaById(id: Long): MediaItem?

    @Insert
    suspend fun insertMedia(item: MediaItem): Long

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMedia(id: Long)

    @Query("DELETE FROM media_items")
    suspend fun deleteAllMedia()

    @Query("DELETE FROM day_entries")
    suspend fun deleteAllDays()
}
