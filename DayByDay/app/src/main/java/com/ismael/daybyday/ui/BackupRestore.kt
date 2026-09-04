package com.ismael.daybyday.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.Backup
import com.ismael.daybyday.data.BackupInfo
import com.ismael.daybyday.data.BackupSummary
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.work.DailyScheduler
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BACKUP_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRANCE)

fun formatBackupDate(millis: Long): String {
    if (millis <= 0L) return "date inconnue"
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    return dateTime.format(BACKUP_DATE_FORMAT)
}

private sealed interface RestoreStep {
    data object Idle : RestoreStep
    data object Scanning : RestoreStep
    data class Found(val info: BackupInfo, val folder: Uri) : RestoreStep
    data object NotFound : RestoreStep
    data object Restoring : RestoreStep
    data class Failed(val message: String) : RestoreStep
}

/**
 * Fait choisir un dossier, y cherche la sauvegarde la plus recente, montre sa
 * date et son contenu, puis restaure apres confirmation.
 *
 * Android revoque l'acces aux dossiers quand l'application est desinstallee :
 * ce choix de dossier est le seul geste que le telephone impose apres une
 * reinstallation. Ensuite le dossier est memorise et la sauvegarde
 * quotidienne repart toute seule.
 */
@Composable
fun FolderBackupRestorer(
    active: Boolean,
    onDismiss: () -> Unit,
    onRestored: (BackupSummary) -> Unit,
) {
    val context = LocalContext.current
    val app = context.dayByDayApp
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<RestoreStep>(RestoreStep.Idle) }

    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri == null) {
            step = RestoreStep.Idle
            onDismiss()
            return@rememberLauncherForActivityResult
        }
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        step = RestoreStep.Scanning
        scope.launch {
            val info = Backup.findLatestInFolder(context, treeUri)
            step = if (info == null) RestoreStep.NotFound else RestoreStep.Found(info, treeUri)
        }
    }

    LaunchedEffect(active) {
        if (active) {
            step = RestoreStep.Idle
            pickFolder.launch(null)
        }
    }

    when (val current = step) {
        RestoreStep.Idle -> Unit

        RestoreStep.Scanning, RestoreStep.Restoring -> AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    if (current == RestoreStep.Scanning) {
                        "Je cherche ta sauvegarde…"
                    } else {
                        "Restauration en cours…"
                    }
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (current == RestoreStep.Scanning) {
                            "Un instant, je regarde ce qu'il y a dans le dossier."
                        } else {
                            "Je remets tes journées, tes notes et tes médias en place."
                        }
                    )
                }
            },
            confirmButton = {},
        )

        is RestoreStep.Found -> AlertDialog(
            onDismissRequest = {
                step = RestoreStep.Idle
                onDismiss()
            },
            title = { Text("Sauvegarde trouvée") },
            text = {
                Column {
                    Text("Dernière sauvegarde le ${formatBackupDate(current.info.exportedAt)}.")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${current.info.days} journée(s) et ${current.info.mediaFiles} média(s)" +
                            if (current.info.name.isNotBlank()) "\n${current.info.name}" else ""
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tout ce qui est actuellement dans l'application sera remplacé " +
                            "par cette sauvegarde.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val info = current.info
                        val folder = current.folder
                        step = RestoreStep.Restoring
                        scope.launch {
                            val result = runCatching {
                                Backup.import(context, app.repository, info.uri)
                            }
                            MediaLoader.clear()
                            result.fold(
                                onSuccess = { summary ->
                                    // Le dossier est memorise : la sauvegarde
                                    // quotidienne peut repartir toute seule.
                                    app.prefs.autoBackupFolder = folder.toString()
                                    app.prefs.autoBackupEnabled = true
                                    app.prefs.firstRunRestoreChecked = true
                                    DailyScheduler.scheduleAutoBackup(context, app.prefs)
                                    step = RestoreStep.Idle
                                    onRestored(summary)
                                },
                                onFailure = { error ->
                                    step = RestoreStep.Failed(error.message ?: "Erreur inconnue")
                                },
                            )
                        }
                    },
                    modifier = Modifier.testTag("confirm-restore"),
                ) { Text("Tout restaurer") }
            },
            dismissButton = {
                TextButton(onClick = {
                    step = RestoreStep.Idle
                    onDismiss()
                }) { Text("Annuler") }
            },
        )

        RestoreStep.NotFound -> AlertDialog(
            onDismissRequest = {
                step = RestoreStep.Idle
                onDismiss()
            },
            title = { Text("Aucune sauvegarde ici") },
            text = {
                Text(
                    "Je n'ai trouvé aucun fichier DayByDay (.zip) dans ce dossier. " +
                        "Essaie un autre dossier, par exemple Téléchargements."
                )
            },
            confirmButton = {
                TextButton(onClick = { pickFolder.launch(null) }) { Text("Choisir un autre dossier") }
            },
            dismissButton = {
                TextButton(onClick = {
                    step = RestoreStep.Idle
                    onDismiss()
                }) { Text("Plus tard") }
            },
        )

        is RestoreStep.Failed -> AlertDialog(
            onDismissRequest = {
                step = RestoreStep.Idle
                onDismiss()
            },
            title = { Text("La restauration a échoué") },
            text = { Text(current.message) },
            confirmButton = {
                TextButton(onClick = {
                    step = RestoreStep.Idle
                    onDismiss()
                }) { Text("Fermer") }
            },
        )
    }
}

/**
 * Ecran affiche au premier lancement quand l'application est vide : il propose
 * de reprendre une sauvegarde existante avant de commencer.
 */
@Composable
fun WelcomeRestoreScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val app = context.dayByDayApp
    var searching by remember { mutableStateOf(false) }
    var restoredMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Bienvenue dans DayByDay",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "L'application est vide pour l'instant. Si tu viens de réinstaller " +
                "et que tu as une sauvegarde, je peux tout remettre en place.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { searching = true },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search-backup"),
        ) {
            Text("Chercher ma sauvegarde")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = {
                app.prefs.firstRunRestoreChecked = true
                onFinished()
            },
            modifier = Modifier.testTag("skip-restore"),
        ) {
            Text("Commencer à zéro")
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Android efface l'autorisation d'accès au dossier quand une " +
                "application est désinstallée : c'est pour ça qu'il faut me " +
                "montrer le dossier une fois. Ensuite je m'en souviens.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        restoredMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
    }

    FolderBackupRestorer(
        active = searching,
        onDismiss = { searching = false },
        onRestored = { summary ->
            searching = false
            restoredMessage =
                "${summary.days} journée(s) et ${summary.mediaFiles} média(s) restaurés."
            onFinished()
        },
    )
}
