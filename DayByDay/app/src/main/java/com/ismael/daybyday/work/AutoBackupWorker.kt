package com.ismael.daybyday.work

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ismael.daybyday.data.Backup
import com.ismael.daybyday.dayByDayApp

/**
 * Sauvegarde automatique quotidienne dans le dossier choisi par l'utilisateur.
 * Le fichier precedent est remplace : une seule sauvegarde occupe la place.
 */
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext.dayByDayApp
        val prefs = app.prefs
        if (!prefs.autoBackupEnabled) return Result.success()
        val folder = prefs.autoBackupFolder ?: return Result.success()

        return runCatching {
            Backup.exportToFolder(applicationContext, app.repository, Uri.parse(folder))
        }.fold(
            onSuccess = {
                prefs.lastAutoBackupAt = System.currentTimeMillis()
                prefs.lastAutoBackupError = null
                Result.success()
            },
            onFailure = { error ->
                prefs.lastAutoBackupError = error.message ?: "Erreur inconnue"
                Result.retry()
            },
        )
    }
}
