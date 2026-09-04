package com.ismael.daybyday.data

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Les quatre couleurs disponibles pour qualifier une journee.
 * [score] sert a calculer les moyennes par semaine / mois / annee.
 */
enum class DayColor(val key: Int, val score: Int, val label: String, val color: Color) {
    GREEN(3, 3, "Bonne journée", Color(0xFF3FBF6A)),
    ORANGE(2, 2, "Journée mitigée", Color(0xFFF2A93B)),
    RED(1, 1, "Journée difficile", Color(0xFFE1483F)),
    BLACK(0, 0, "Journée très noire", Color(0xFF15171D));

    companion object {
        val MAX_SCORE = GREEN.score

        fun fromKey(key: Int?): DayColor? = entries.firstOrNull { it.key == key }

        /** Couleur representant une moyenne (0..3), utilisee pour les resumes. */
        fun fromAverage(average: Double): Color {
            val clamped = average.coerceIn(0.0, MAX_SCORE.toDouble())
            val stops = listOf(
                0.0 to BLACK.color,
                1.0 to RED.color,
                2.0 to ORANGE.color,
                3.0 to GREEN.color,
            )
            for (i in 0 until stops.size - 1) {
                val (lowValue, lowColor) = stops[i]
                val (highValue, highColor) = stops[i + 1]
                if (clamped <= highValue) {
                    val t = ((clamped - lowValue) / (highValue - lowValue)).toFloat()
                    return lerpColor(lowColor, highColor, t)
                }
            }
            return GREEN.color
        }

        private fun lerpColor(from: Color, to: Color, t: Float): Color = Color(
            red = from.red + (to.red - from.red) * t,
            green = from.green + (to.green - from.green) * t,
            blue = from.blue + (to.blue - from.blue) * t,
            alpha = 1f,
        )
    }
}

/**
 * Une journee du calendrier. La cle primaire est le numero de jour epoch
 * (LocalDate.toEpochDay) : simple a trier et a interroger par plage.
 */
@Entity(tableName = "day_entries")
data class DayEntry(
    @PrimaryKey val epochDay: Long,
    val colorKey: Int? = null,
    val title: String = "",
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val color: DayColor? get() = DayColor.fromKey(colorKey)

    val isEmpty: Boolean get() = colorKey == null && title.isBlank() && note.isBlank()
}

enum class MediaKind { PHOTO, VIDEO }

/** Une photo ou video rattachee a une journee, stockee dans le dossier prive de l'app. */
@Entity(
    tableName = "media_items",
    indices = [Index("epochDay")],
)
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    /** Chemin relatif au dossier "media" du stockage interne. */
    val relativePath: String,
    val kindKey: Int,
    val addedAt: Long = System.currentTimeMillis(),
) {
    val kind: MediaKind get() = if (kindKey == 1) MediaKind.VIDEO else MediaKind.PHOTO
}

/** Nombre de medias par jour, pour afficher une pastille dans le calendrier. */
data class DayMediaCount(val epochDay: Long, val count: Int)
