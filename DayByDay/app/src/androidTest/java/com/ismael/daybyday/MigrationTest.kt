package com.ismael.daybyday

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismael.daybyday.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifie que les journees deja enregistrees avec la version 1 de la base
 * (avant les categories sport / alimentation / sorties et les etiquettes)
 * sont bien conservees apres la mise a jour de l'application.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val databaseName = "migration-test.db"

    @Test
    fun lesDonneesDeLaVersion1SurviventALaMigration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        context.deleteDatabase(databaseName)

        // Base telle que la version 1 de l'application la creait.
        val legacy = SQLiteDatabase.openOrCreateDatabase(file, null)
        legacy.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_entries` (" +
                "`epochDay` INTEGER NOT NULL, `colorKey` INTEGER, `title` TEXT NOT NULL, " +
                "`note` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))"
        )
        legacy.execSQL(
            "CREATE TABLE IF NOT EXISTS `media_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`relativePath` TEXT NOT NULL, `kindKey` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL)"
        )
        legacy.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_items_epochDay` ON `media_items` (`epochDay`)"
        )
        legacy.execSQL(
            "INSERT INTO day_entries (epochDay, colorKey, title, note, updatedAt) " +
                "VALUES (20000, 2, 'Retour à la maison', 'Hier je suis allé chez Dune', 1)"
        )
        legacy.execSQL(
            "INSERT INTO media_items (epochDay, relativePath, kindKey, addedAt) " +
                "VALUES (20000, '2026/09/photo.jpg', 0, 1)"
        )
        legacy.version = 1
        legacy.close()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

        try {
            runBlocking {
                val dao = database.dayDao()
                val day = dao.dayOnce(20000)
                assertEquals("Retour à la maison", day?.title)
                assertEquals(2, day?.colorKey)
                // Les nouveaux champs existent et sont simplement vides.
                assertEquals(null, day?.sportLevel)
                assertEquals(null, day?.weightKg)
                assertEquals(1, dao.mediaForDay(20000).size)
                // Les etiquettes par defaut sont ajoutees par la migration,
                // et rangees dans leur famille par la suivante.
                val tags = dao.allTags()
                assertTrue(tags.isNotEmpty())
                assertTrue(tags.any { it.category != null })
                // La table des mouvements d'argent est utilisable.
                dao.upsertMoney(
                    com.ismael.daybyday.data.MoneyEntry(
                        epochDay = 20000,
                        amountCents = -1250,
                        label = "Test",
                    )
                )
                assertEquals(1, dao.allMoney().size)
                // Les moments de la journee existent et sont vides.
                assertEquals(null, day?.partMorning)
                assertEquals(true, day?.colorManual)
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
