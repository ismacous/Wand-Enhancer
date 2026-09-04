package com.ismael.daybyday

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismael.daybyday.data.Backup
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DayRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Verifie qu'une sauvegarde peut etre relue sans etre restauree : c'est ce qui
 * permet d'annoncer "derniere sauvegarde le ..." avant de demander confirmation.
 */
@RunWith(AndroidJUnit4::class)
class BackupTest {

    @Test
    fun exporterPuisRelireLaFicheDeLaSauvegarde() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DayRepository(context)

        // Un jour tres ancien pour ne pas perturber les tests d'interface.
        val day = LocalDate.now().minusDays(500)
        repository.saveDay(
            DayEntry(
                epochDay = day.toEpochDay(),
                colorKey = DayColor.GREEN.key,
                title = "Journée de test",
            )
        )

        val file = File(context.cacheDir, "backup-test.zip")
        val summary = Backup.export(context, repository, Uri.fromFile(file))
        val info = Backup.peek(context, Uri.fromFile(file), file.name)

        assertTrue(summary.days >= 1)
        assertEquals(summary.days, info?.days)
        assertEquals(summary.mediaFiles, info?.mediaFiles)
        assertTrue((info?.exportedAt ?: 0L) > 0L)
        assertEquals("backup-test.zip", info?.name)

        file.delete()
    }

    @Test
    fun unFichierQuiNEstPasUneSauvegardeEstIgnore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "pas-une-sauvegarde.zip")
        file.writeText("ceci n'est pas un zip")

        assertEquals(null, Backup.peek(context, Uri.fromFile(file), file.name))

        file.delete()
    }
}
