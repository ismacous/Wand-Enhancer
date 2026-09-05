package com.ismael.daybyday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DayEntry::class,
        MediaItem::class,
        Tag::class,
        DayTagCrossRef::class,
        MoneyEntry::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dayDao(): DayDao

    companion object {
        private const val NAME = "daybyday.db"

        /** Etiquettes proposees au premier lancement ; tout est modifiable ensuite. */
        /** emoji, nom, famille. */
        val DEFAULT_TAGS = listOf(
            Triple("😴", "Bien dormi", TagCategory.SLEEP),
            Triple("🥱", "Mal dormi", TagCategory.SLEEP),
            Triple("👥", "Ami·es", TagCategory.SOCIAL),
            Triple("🏠", "Famille", TagCategory.SOCIAL),
            Triple("💬", "Copine", TagCategory.SOCIAL),
            Triple("🚶", "Marche", TagCategory.ACTIVITY),
            Triple("🌳", "Dehors / nature", TagCategory.ACTIVITY),
            Triple("🍳", "Cuisine maison", TagCategory.FOOD),
            Triple("🍟", "Fast-food", TagCategory.FOOD),
            Triple("💼", "Recherche d'emploi", TagCategory.WORK),
            Triple("📱", "Écrans +++", TagCategory.SCREENS),
            Triple("🎮", "Jeux vidéo", TagCategory.SCREENS),
        )

        /** Insertion pour une base fraiche, qui possede deja la colonne category. */
        private fun seedTags(db: SupportSQLiteDatabase) {
            DEFAULT_TAGS.forEachIndexed { index, (emoji, name, category) ->
                db.execSQL(
                    "INSERT INTO tags (name, emoji, sortOrder, category) VALUES (?, ?, ?, ?)",
                    arrayOf<Any>(name, emoji, index, category.key),
                )
            }
        }

        /**
         * Insertion au format de la version 2 : la colonne category n'existe pas
         * encore a ce stade, elle est ajoutee par la migration suivante.
         */
        private fun seedTagsWithoutCategory(db: SupportSQLiteDatabase) {
            DEFAULT_TAGS.forEachIndexed { index, (emoji, name, _) ->
                db.execSQL(
                    "INSERT INTO tags (name, emoji, sortOrder) VALUES (?, ?, ?)",
                    arrayOf<Any>(name, emoji, index),
                )
            }
        }

        /** Range les etiquettes deja creees dans leur famille. */
        private fun categorizeExistingTags(db: SupportSQLiteDatabase) {
            DEFAULT_TAGS.forEach { (_, name, category) ->
                db.execSQL(
                    "UPDATE tags SET category = ? WHERE name = ? AND category IS NULL",
                    arrayOf<Any>(category.key, name),
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
                seedTagsWithoutCategory(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_entries ADD COLUMN partMorning INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN partAfternoon INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN partEvening INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN partNight INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN colorManual INTEGER")
                db.execSQL("UPDATE day_entries SET colorManual = 1 WHERE colorKey IS NOT NULL")
                db.execSQL("ALTER TABLE tags ADD COLUMN category TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transactions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, " +
                        "`amountCents` INTEGER NOT NULL, " +
                        "`label` TEXT NOT NULL, " +
                        "`categoryKey` TEXT, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_epochDay` " +
                        "ON `transactions` (`epochDay`)"
                )
                categorizeExistingTags(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_entries ADD COLUMN steps INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN screenMinutes INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tags ADD COLUMN slug TEXT")
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { instance = it }
        }
    }
}
