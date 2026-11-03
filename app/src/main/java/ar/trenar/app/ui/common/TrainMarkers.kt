package ar.trenar.app.ui.common

import android.graphics.drawable.Drawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Duration of the glide between two refreshed positions of the same train. */
const val TRAIN_GLIDE_MS = 2_500

/** Jumps longer than this (a re-used service id, a re-routed trip) teleport instead of gliding. */
private const val MAX_GLIDE_METERS = 5_000.0

/** A train to draw this frame: [key] identifies the same train across refreshes. */
data class TrainSpot(val key: String, val point: GeoPoint, val icon: Drawable, val title: String)

/**
 * Keeps one osmdroid [Marker] per train alive across refreshes, so a new position can be animated
 * from wherever the marker currently sits instead of rebuilding (and blinking) the overlay.
 */
class TrainMarkers {
    private class Entry(val marker: Marker, var from: GeoPoint, var to: GeoPoint)

    private val entries = LinkedHashMap<String, Entry>()

    /** Markers in draw order, to add to the map's overlays. */
    val markers: List<Marker> get() = entries.values.map { it.marker }

    /**
     * Syncs the markers with [spots]: new trains appear at their position, gone ones are dropped,
     * and moved ones get a new glide target starting from where they're drawn right now.
     */
    fun sync(map: MapView, spots: List<TrainSpot>) {
        val keys = spots.mapTo(HashSet()) { it.key }
        entries.keys.retainAll(keys)
        spots.forEach { s ->
            val e = entries[s.key]
            if (e == null) {
                val marker = Marker(map).apply {
                    position = s.point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                }
                entries[s.key] = Entry(marker, s.point, s.point)
            } else if (!samePoint(e.to, s.point)) {
                val current = e.marker.position.clone()
                e.from = if (current.distanceToAsDouble(s.point) > MAX_GLIDE_METERS) s.point else current
                e.to = s.point
            }
            entries.getValue(s.key).marker.apply {
                icon = s.icon
                title = s.title
            }
        }
    }

    /** Places every marker at [fraction] (0..1) of the way along its current glide. */
    fun step(fraction: Float) {
        val f = fraction.toDouble().coerceIn(0.0, 1.0)
        entries.values.forEach { e ->
            e.marker.position = GeoPoint(
                e.from.latitude + (e.to.latitude - e.from.latitude) * f,
                e.from.longitude + (e.to.longitude - e.from.longitude) * f,
            )
        }
    }

    /** Ends every glide where it's headed (no animation left pending). */
    fun settle() {
        entries.values.forEach { e ->
            e.from = e.to
            e.marker.position = e.to
        }
    }

    fun clear() = entries.clear()

    private fun samePoint(a: GeoPoint, b: GeoPoint) =
        a.latitude == b.latitude && a.longitude == b.longitude
}
