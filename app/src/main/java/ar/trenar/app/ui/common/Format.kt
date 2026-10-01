package ar.trenar.app.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Shared formatting helpers for arrival times. */
object Format {

    private val AR_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Scheduled time in Buenos Aires local time, e.g. "07:42". */
    fun localTime(instant: Instant?): String? =
        instant?.atZone(AR_ZONE)?.format(HHMM)

    /** Compact ETA label from seconds-to-arrival. */
    fun eta(seconds: Int?): String = when {
        seconds == null -> "s/d"
        seconds <= 30 -> "Llegando"
        seconds < 60 -> "< 1 min"
        else -> "${seconds / 60} min"
    }

    /** Big split for the countdown: value + unit. */
    fun etaBig(seconds: Int?): Pair<String, String> = when {
        seconds == null -> "–" to ""
        seconds <= 30 -> "¡Ya!" to ""
        seconds < 120 -> "${seconds}" to "seg"
        else -> "${seconds / 60}" to "min"
    }

    fun freshness(secondsAgo: Long): String = when {
        secondsAgo < 5 -> "recién"
        secondsAgo < 60 -> "hace ${secondsAgo}s"
        else -> "hace ${secondsAgo / 60} min"
    }
}
