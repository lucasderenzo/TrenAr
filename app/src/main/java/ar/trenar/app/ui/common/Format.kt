package ar.trenar.app.ui.common

import android.content.Context
import ar.trenar.app.R
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
    fun eta(ctx: Context, seconds: Int?): String = when {
        seconds == null -> ctx.getString(R.string.sin_datos)
        seconds <= 30 -> ctx.getString(R.string.llegando)
        seconds < 60 -> ctx.getString(R.string.menos_1_min)
        else -> ctx.getString(R.string.min_value, seconds / 60)
    }

    /** Big split for the countdown: value + unit. */
    fun etaBig(ctx: Context, seconds: Int?): Pair<String, String> = when {
        seconds == null -> "–" to ""
        seconds <= 30 -> ctx.getString(R.string.ya) to ""
        seconds < 120 -> "$seconds" to ctx.getString(R.string.seg)
        else -> "${seconds / 60}" to ctx.getString(R.string.min)
    }

    fun freshness(ctx: Context, secondsAgo: Long): String = when {
        secondsAgo < 5 -> ctx.getString(R.string.recien)
        secondsAgo < 60 -> ctx.getString(R.string.hace_seg, secondsAgo.toInt())
        else -> ctx.getString(R.string.hace_min, (secondsAgo / 60).toInt())
    }
}
