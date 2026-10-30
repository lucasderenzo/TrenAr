package ar.trenar.app.ui.common

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.graphics.RectF
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ar.trenar.app.R
import ar.trenar.app.data.LineTerminals
import ar.trenar.app.data.model.StationRef
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** A live train to draw on a station mini-map. [headingEast] orients the side-view sprite. */
data class MiniTrain(
    val lat: Double,
    val lng: Double,
    val colorArgb: Int,
    val label: String,
    val headingEast: Boolean = true,
)

/** One station of the line drawn on the mini-map, with its plain and labeled marker art. */
private class LineStop(
    val marker: Marker,
    val plain: MarkerArt,
    val labeled: MarkerArt,
    val point: GeoPoint,
    /** Lower = labeled first when names would overlap (the focused station, then terminals). */
    val priority: Int,
)

/**
 * A small OpenStreetMap view centered on a station, with a pin at its location and,
 * optionally, live train markers. Fits the view to include [trains] when there are any.
 *
 * When [lineStations] is given, the rest of the line is drawn too (like "Red en vivo"), so that
 * pinching out shows every station with its name; names are decluttered by zoom level so they
 * never overlap. [extraTrains] are drawn but don't take part in the initial auto-zoom.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun StationMiniMap(
    lat: Double,
    lng: Double,
    label: String,
    modifier: Modifier = Modifier,
    trains: List<MiniTrain> = emptyList(),
    lineStations: List<StationRef> = emptyList(),
    lineColorArgb: Int = AndroidColor.GRAY,
    extraTrains: List<MiniTrain> = emptyList(),
) {
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density
    val stops = remember { mutableListOf<LineStop>() }
    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(15.5)
            controller.setCenter(GeoPoint(lat, lng))
        }
    }
    val lastKey = remember { mutableStateOf("") }
    val lastFitKey = remember { mutableStateOf("") }
    // Once the user pinches/pans the map, periodic refreshes must not undo it with an auto-zoom.
    val userMoved = remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        mapView.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                // A second finger means a pinch: keep it on the map instead of the screen's scroll.
                // A single-finger drag is left alone so the screen still scrolls as before.
                MotionEvent.ACTION_POINTER_DOWN -> {
                    userMoved.value = true
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                // The map got the whole gesture (pan / tap / double-tap), not stolen by the scroll.
                MotionEvent.ACTION_UP -> userMoved.value = true
            }
            false
        }
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?) = false
            override fun onZoom(event: ZoomEvent?): Boolean {
                declutterLabels(mapView, stops)
                return false
            }
        }
        mapView.addMapListener(listener)
        mapView.onResume()
        onDispose {
            mapView.removeMapListener(listener)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            val fitKey = buildString {
                append(lat).append(',').append(lng)
                trains.forEach { append('|').append(it.lat).append(',').append(it.lng) }
            }
            val key = buildString {
                append(fitKey).append('#').append(lineColorArgb)
                lineStations.forEach { append(';').append(it.id) }
                extraTrains.forEach { append('|').append(it.lat).append(',').append(it.lng) }
            }
            if (key == lastKey.value) return@AndroidView
            lastKey.value = key

            mv.overlays.clear()
            stops.clear()
            val terminalTag = ctx.getString(R.string.est_terminal)
            val focusedId = lineStations.firstOrNull { it.lat == lat && it.lng == lng }?.id
            lineStations.forEach { st ->
                val focused = st.id == focusedId
                val terminal = LineTerminals.isTerminal(st.line, st.name)
                // terminals: double-size circle, and "Est. Terminal" above the name when it fits
                val sizeDp = if (terminal) 28f else 14f
                val plain = buildStationMarker(
                    ctx, density, lineColorArgb, sizeDp, AndroidColor.WHITE, 1.5f, R.drawable.ic_stat_train, null,
                )
                val labeled = buildStationMarker(
                    ctx, density, lineColorArgb, sizeDp, AndroidColor.WHITE, 1.5f, R.drawable.ic_stat_train, st.name,
                    if (terminal) terminalTag else null,
                )
                val point = GeoPoint(st.lat, st.lng)
                val marker = Marker(mv).apply {
                    position = point
                    icon = plain.drawable
                    setAnchor(Marker.ANCHOR_CENTER, plain.anchorY)
                    title = st.name
                    setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                }
                mv.overlays.add(marker)
                stops += LineStop(marker, plain, labeled, point, if (focused) 0 else if (terminal) 1 else 2)
            }

            Marker(mv).apply {
                position = GeoPoint(lat, lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = label
                setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
            }.also { mv.overlays.add(it) }

            (trains + extraTrains).forEach { t ->
                val icon = buildTrainSprite(ctx, density, t.colorArgb, 30f, flip = !t.headingEast)
                Marker(mv).apply {
                    position = GeoPoint(t.lat, t.lng)
                    this.icon = icon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = t.label
                    setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                }.also { mv.overlays.add(it) }
            }
            declutterLabels(mv, stops)
            mv.invalidate()

            if (userMoved.value || fitKey == lastFitKey.value) return@AndroidView
            lastFitKey.value = fitKey
            if (trains.isEmpty()) {
                mv.controller.setZoom(15.5)
                mv.controller.setCenter(GeoPoint(lat, lng))
            } else {
                val pts = listOf(GeoPoint(lat, lng)) + trains.map { GeoPoint(it.lat, it.lng) }
                mv.post { runCatching { mv.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(pts), false, 90) } }
            }
        },
    )
}

/**
 * Shows station names only where they fit at the current zoom: stations are labeled in priority
 * order (focused station, terminals, then the rest) and a name is skipped when its box would
 * overlap one already placed. Overlap only depends on zoom, so this runs on zoom changes.
 */
private fun declutterLabels(mv: MapView, stops: List<LineStop>) {
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
            val fits = placed.none { RectF.intersects(it, box) }
            val art = if (fits) stop.labeled else stop.plain
            if (fits) placed += box
            if (stop.marker.icon !== art.drawable) {
                stop.marker.icon = art.drawable
                stop.marker.setAnchor(Marker.ANCHOR_CENTER, art.anchorY)
            }
        }
    mv.invalidate()
}
