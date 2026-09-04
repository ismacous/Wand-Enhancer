package com.ismael.daybyday.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSummary(val days: Int, val mediaFiles: Int)

/**
 * Sauvegarde / restauration complete sous forme d'un fichier .zip choisi par
 * l'utilisateur (aucun envoi reseau : c'est un simple fichier local).
 */
object Backup {

    private const val JSON_NAME = "daybyday.json"
    private const val MEDIA_PREFIX = "media/"
    private const val FORMAT_VERSION = 1

    suspend fun export(context: Context, repository: DayRepository, target: Uri): BackupSummary =
        withContext(Dispatchers.IO) {
            val days = repository.allDays()
            val mediaItems = repository.allMedia()

            val root = JSONObject()
            root.put("version", FORMAT_VERSION)
            root.put("exportedAt", System.currentTimeMillis())

            val daysJson = JSONArray()
            days.forEach { day ->
                daysJson.put(
                    JSONObject()
                        .put("epochDay", day.epochDay)
                        .put("colorKey", day.colorKey ?: JSONObject.NULL)
                        .put("title", day.title)
                        .put("note", day.note)
                        .put("updatedAt", day.updatedAt)
                )
            }
            root.put("days", daysJson)

            val mediaJson = JSONArray()
            mediaItems.forEach { item ->
                mediaJson.put(
                    JSONObject()
                        .put("epochDay", item.epochDay)
                        .put("relativePath", item.relativePath)
                        .put("kindKey", item.kindKey)
                        .put("addedAt", item.addedAt)
                )
            }
            root.put("media", mediaJson)

            var copied = 0
            val output = context.contentResolver.openOutputStream(target)
                ?: error("Impossible d'ouvrir le fichier de destination.")
            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(JSON_NAME))
                zip.write(root.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                mediaItems.forEach { item ->
                    val file = repository.media.file(item.relativePath)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry(MEDIA_PREFIX + item.relativePath))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        copied += 1
                    }
                }
            }
            BackupSummary(days = days.size, mediaFiles = copied)
        }

    suspend fun import(context: Context, repository: DayRepository, source: Uri): BackupSummary =
        withContext(Dispatchers.IO) {
            val staging = File(context.cacheDir, "restore_${System.currentTimeMillis()}")
            staging.mkdirs()
            var json: JSONObject? = null
            var restoredFiles = 0

            try {
                val input = context.contentResolver.openInputStream(source)
                    ?: error("Impossible d'ouvrir la sauvegarde.")
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        when {
                            entry.isDirectory -> Unit
                            name == JSON_NAME ->
                                json = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                            name.startsWith(MEDIA_PREFIX) -> {
                                val relative = name.removePrefix(MEDIA_PREFIX)
                                val destination = safeChild(staging, relative)
                                if (destination != null) {
                                    destination.parentFile?.mkdirs()
                                    destination.outputStream().use { zip.copyTo(it) }
                                    restoredFiles += 1
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                val payload = json ?: error("Fichier de sauvegarde invalide (daybyday.json manquant).")

                val days = mutableListOf<DayEntry>()
                val daysJson = payload.optJSONArray("days") ?: JSONArray()
                for (i in 0 until daysJson.length()) {
                    val item = daysJson.getJSONObject(i)
                    days += DayEntry(
                        epochDay = item.getLong("epochDay"),
                        colorKey = if (item.isNull("colorKey")) null else item.getInt("colorKey"),
                        title = item.optString("title", ""),
                        note = item.optString("note", ""),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                    )
                }

                val mediaItems = mutableListOf<MediaItem>()
                val mediaJson = payload.optJSONArray("media") ?: JSONArray()
                for (i in 0 until mediaJson.length()) {
                    val item = mediaJson.getJSONObject(i)
                    mediaItems += MediaItem(
                        epochDay = item.getLong("epochDay"),
                        relativePath = item.getString("relativePath"),
                        kindKey = item.optInt("kindKey", 0),
                        addedAt = item.optLong("addedAt", System.currentTimeMillis()),
                    )
                }

                repository.media.deleteAll()
                staging.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relative = file.relativeTo(staging).path.replace(File.separatorChar, '/')
                    file.inputStream().use { repository.media.writeFrom(relative, it) }
                }
                repository.replaceAll(days, mediaItems)

                BackupSummary(days = days.size, mediaFiles = restoredFiles)
            } finally {
                staging.deleteRecursively()
            }
        }

    /** Empeche un chemin malveillant du type ../../ de sortir du dossier cible. */
    private fun safeChild(root: File, relative: String): File? {
        val candidate = File(root, relative).canonicalFile
        return if (candidate.path.startsWith(root.canonicalFile.path + File.separator)) candidate else null
    }

    /**
     * Export texte d'une annee : pratique pour preparer la video annuelle de
     * fin d'annee (titres + notes, jour par jour, avec les statistiques).
     */
    suspend fun exportYearText(
        context: Context,
        repository: DayRepository,
        year: Int,
        target: Uri,
    ): Int = withContext(Dispatchers.IO) {
        val start = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)
        val days = repository.allDays()
            .filter { it.epochDay in start.toEpochDay()..end.toEpochDay() }
            .sortedBy { it.epochDay }
        val mediaByDay = repository.allMedia().groupBy { it.epochDay }

        val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)
        val builder = StringBuilder()
        builder.appendLine("# DayByDay — $year")
        builder.appendLine()

        val yearSummary = Stats.summarize(year.toString(), days, if (start.isLeapYear) 366 else 365)
        builder.appendLine("Jours notés : ${yearSummary.filledDays} / ${yearSummary.totalDays}")
        yearSummary.average?.let { builder.appendLine("Moyenne de l'année : ${"%.2f".format(it)} / 3") }
        DayColor.entries.forEach { color ->
            builder.appendLine("- ${color.label} : ${yearSummary.countOf(color)} jour(s)")
        }
        builder.appendLine()

        (1..12).forEach { monthValue ->
            val monthDays = days.filter { LocalDate.ofEpochDay(it.epochDay).monthValue == monthValue }
            val monthName = LocalDate.of(year, monthValue, 1)
                .month.getDisplayName(TextStyle.FULL, Locale.FRANCE)
                .replaceFirstChar { it.uppercase() }
            val summary = Stats.summarize(
                monthName,
                monthDays,
                LocalDate.of(year, monthValue, 1).lengthOfMonth(),
            )
            builder.appendLine("## $monthName")
            builder.appendLine(
                summary.average?.let { "Moyenne : ${"%.2f".format(it)} / 3 — ${summary.filledDays} jour(s) noté(s)" }
                    ?: "Aucun jour noté."
            )
            builder.appendLine()
            monthDays.forEach { entry ->
                val date = LocalDate.ofEpochDay(entry.epochDay)
                val label = entry.color?.label ?: "Sans couleur"
                builder.appendLine("### ${date.format(dayFormat)} — $label")
                if (entry.title.isNotBlank()) builder.appendLine("**${entry.title}**")
                if (entry.note.isNotBlank()) builder.appendLine(entry.note)
                val mediaCount = mediaByDay[entry.epochDay]?.size ?: 0
                if (mediaCount > 0) builder.appendLine("_($mediaCount média(s) dans l'application)_")
                builder.appendLine()
            }
        }

        val output = context.contentResolver.openOutputStream(target)
            ?: error("Impossible d'ouvrir le fichier de destination.")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(builder.toString()) }
        days.size
    }
}
