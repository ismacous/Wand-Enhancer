package com.ismael.daybyday.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

/**
 * Gestion des fichiers photos / videos. Tout est copie dans le stockage
 * interne prive de l'application (/data/data/<package>/files/media), donc
 * invisible pour la galerie et pour les autres applications.
 */
class MediaFiles(private val context: Context) {

    val root: File get() = File(context.filesDir, "media").apply { if (!exists()) mkdirs() }

    fun file(relativePath: String): File = File(root, relativePath)

    /** Copie le contenu de [uri] dans le stockage prive et renvoie le media cree. */
    fun importFrom(uri: Uri, epochDay: Long): MediaItem? {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val kind = if (mime.startsWith("video")) MediaKind.VIDEO else MediaKind.PHOTO
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: if (kind == MediaKind.VIDEO) "mp4" else "jpg"

        val date = LocalDate.ofEpochDay(epochDay)
        val relativeDir = "%04d/%02d".format(date.year, date.monthValue)
        val relativePath = "$relativeDir/${UUID.randomUUID()}.$extension"
        val target = File(root, relativePath)
        target.parentFile?.mkdirs()

        val stream: InputStream = runCatching { context.contentResolver.openInputStream(uri) }
            .getOrNull() ?: return null
        stream.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        if (target.length() == 0L) {
            target.delete()
            return null
        }
        return MediaItem(
            epochDay = epochDay,
            relativePath = relativePath,
            kindKey = if (kind == MediaKind.VIDEO) 1 else 0,
        )
    }

    /** Ecrit un fichier venant d'une sauvegarde. */
    fun writeFrom(relativePath: String, input: InputStream) {
        val target = File(root, relativePath)
        target.parentFile?.mkdirs()
        target.outputStream().use { output -> input.copyTo(output) }
    }

    fun delete(relativePath: String) {
        val target = File(root, relativePath)
        if (target.exists()) target.delete()
        // Nettoie les dossiers vides laisses derriere.
        var parent = target.parentFile
        while (parent != null && parent != root && parent.isDirectory && parent.list()?.isEmpty() == true) {
            parent.delete()
            parent = parent.parentFile
        }
    }

    fun deleteAll() {
        root.deleteRecursively()
        root.mkdirs()
    }

    fun totalBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
