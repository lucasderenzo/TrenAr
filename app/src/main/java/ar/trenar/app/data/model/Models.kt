package ar.trenar.app.data.model

import java.time.Instant

/** A station from the offline catalog (or a live search), optionally with distance. */
data class StationRef(
    val id: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val ramales: List<Int> = emptyList(),
    val distanceMeters: Double? = null,
)

enum class DelayStatus { ON_TIME, MINOR, MAJOR, CANCELLED, UNKNOWN }

/** A single upcoming train at a station. */
data class Arrival(
    val serviceId: String,
    val destination: String,
    val ramalName: String,
    val line: String,
    val platform: String?,
    val etaSeconds: Int?,
    /** Local (device) epoch seconds when this snapshot was mapped — used for ticking. */
    val fetchedAtEpochSec: Long,
    val scheduledUtc: Instant?,
    /** Positive => running late (seconds). */
    val delaySeconds: Long?,
    val toleranceSeconds: Int?,
    val cancelled: Boolean,
    val direction: Int?,
    val trainLat: Double?,
    val trainLng: Double?,
    val note: String?,
) {
    fun status(): DelayStatus {
        if (cancelled) return DelayStatus.CANCELLED
        val d = delaySeconds ?: return DelayStatus.UNKNOWN
        val tol = (toleranceSeconds ?: 180).coerceAtLeast(120).toLong()
        return when {
            d <= tol -> DelayStatus.ON_TIME
            d <= 600 -> DelayStatus.MINOR
            else -> DelayStatus.MAJOR
        }
    }

    /** Delay in whole minutes (rounded), only when meaningfully late. */
    fun delayMinutes(): Int? {
        val d = delaySeconds ?: return null
        return if (d < 60) null else ((d + 30) / 60).toInt()
    }

    /** Seconds until arrival relative to [nowEpochSec], clamped at 0. */
    fun etaSecondsAt(nowEpochSec: Long): Int? {
        val eta = etaSeconds ?: return null
        val elapsed = nowEpochSec - fetchedAtEpochSec
        return (eta - elapsed).coerceAtLeast(0).toInt()
    }
}

/** Result of loading a station board. */
data class BoardState(
    val station: StationRef?,
    val arrivals: List<Arrival>,
    val serverTimestamp: Long,
    val loadedAtEpochSec: Long,
)
