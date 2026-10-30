package ar.trenar.app.ui.common

import android.graphics.RectF
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** One station of a line drawn on a map, with its plain and labeled marker art. */
class LineStop(
    val marker: Marker,
    val plain: MarkerArt,
    val labeled: MarkerArt,
    val point: GeoPoint,
    /** Lower = labeled first when names would overlap (e.g. the focused station, then terminals). */
    val priority: Int,
    /** Always labeled, even if its name overlaps another one (e.g. terminals on "Red en vivo"). */
    val pinned: Boolean = false,
)

/**
 * Shows station names only where they fit at the current zoom: stations are labeled in priority
 * order and a name is skipped when its box would overlap one already placed ([LineStop.pinned]
 * stops are labeled regardless). Overlap only depends on zoom, so this runs on zoom changes
 * (and when the stations change).
 */
fun declutterLabels(mv: MapView, stops: List<LineStop>) {
    if (stops.isEmpty()) return
    val mapSize = TileSystem.MapSize(mv.zoomLevelDouble)
    val ts = MapView.getTileSystem()
    val placed = mutableListOf<RectF>()
    stops.withIndex()
        .sortedWith(compareBy({ it.value.priority }, { it.index }))
        .forEach { (_, stop) ->
            val x = ts.getMercatorXFromLongitude(stop.point.longitude, mapSize, false).toFloat()
            val y = ts.getMercatorYFromLatitude(stop.point.latitude, mapSize, false).toFloat()
            val d = stop.labeled.drawable
            val w = d.intrinsicWidth.toFloat()
            val h = d.intrinsicHeight.toFloat()
            val top = y - stop.labeled.anchorY * h
            val box = RectF(x - w / 2f, top, x + w / 2f, top + h)
            val fits = stop.pinned || placed.none { RectF.intersects(it, box) }
            val art = if (fits) stop.labeled else stop.plain
            if (fits) placed += box
            if (stop.marker.icon !== art.drawable) {
                stop.marker.icon = art.drawable
                stop.marker.setAnchor(Marker.ANCHOR_CENTER, art.anchorY)
            }
        }
    mv.invalidate()
}
