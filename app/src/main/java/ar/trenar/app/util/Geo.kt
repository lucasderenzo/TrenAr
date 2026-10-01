package ar.trenar.app.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Small geo helpers, no external deps. */
object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance in meters. */
    fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Initial bearing from point 1 to point 2, in degrees clockwise from North [0,360). */
    fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lng2 - lng1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        val deg = Math.toDegrees(atan2(y, x))
        return (deg + 360.0) % 360.0
    }

    /** Human-friendly distance in Spanish, e.g. "450 m", "1,2 km". */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) "${(meters / 10).roundToInt() * 10} m"
        else {
            val km = meters / 1000.0
            val s = String.format("%.1f", km).replace('.', ',')
            "$s km"
        }
    }
}
