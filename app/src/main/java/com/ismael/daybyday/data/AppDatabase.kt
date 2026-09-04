package com.ismael.daybyday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DayEntry::class, MediaItem::class, Tag::class, DayTagCrossRef::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dayDao(): DayDao

    companion object {
        private const val NAME = "daybyday.db"

        /** Etiquettes proposees au premier lancement ; tout est modifiable ensuite. */
        val DEFAULT_TAGS = listOf(
            "😴" to "Bien dormi",
            "🥱" to "Mal dormi",
            "🚶" to "Marche",
            "👥" to "Ami·es",
            "🏠" to "Famille",
            "💼" to "Travail",
            "🍳" to "Cuisine maison",
            "🍟" to "Fast-food",
            "📱" to "Écrans +++",
            "🌳" to "Dehors / nature",
        )

        private fun seedTags(db: SupportSQLiteDatabase) {
            DEFAULT_TAGS.forEachIndexed { index, (emoji, name) ->
                db.execSQL(
                    "INSERT INTO tags (name, emoji, sortOrder) VALUES (?, ?, ?)",
                    arrayOf<Any>(name, emoji, index),
                )
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_entries ADD COLUMN sportLevel INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN foodLevel INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN wentOut INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN weightKg REAL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tags` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`emoji` TEXT NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `day_tags` (" +
                        "`epochDay` INTEGER NOT NULL, " +
                        "`tagId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`epochDay`, `tagId`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tags_tagId` ON `day_tags` (`tagId`)")
                seedTags(db)
            }
        }

        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedTags(db)
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(seedCallback)
                .build()
                .also { instance = it }
        }
    }
}
