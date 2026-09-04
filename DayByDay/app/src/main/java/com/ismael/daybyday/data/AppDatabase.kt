package com.ismael.daybyday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DayEntry::class, MediaItem::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dayDao(): DayDao

    companion object {
        private const val NAME = "daybyday.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                NAME,
            ).build().also { instance = it }
        }

        /** Ferme la base (utilise avant de restaurer une sauvegarde). */
        fun closeForRestore() = synchronized(this) {
            instance?.close()
            instance = null
        }

        fun databaseFiles(context: Context): List<java.io.File> = listOf(
            context.getDatabasePath(NAME),
            context.getDatabasePath("$NAME-wal"),
            context.getDatabasePath("$NAME-shm"),
        )
    }
}
