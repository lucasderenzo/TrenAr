package ar.trenar.app.ui.common

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
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
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * A live train to draw on a station mini-map. [headingEast] orients the side-view sprite.
 * [key] identifies the same train across refreshes so its marker glides to the new position;
 * when blank, the train's index in its list is used instead.
 */
data class MiniTrain(
    val lat: Double,
    val lng: Double,
    val colorArgb: Int,
    val label: String,
    val headingEast: Boolean = true,
    val key: String = "",
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
    // Station markers and the pin are rebuilt only when the station or its line change, not on
    // every train refresh, so the map doesn't blink and names aren't decluttered again for nothing.
    val stationsKey = remember { mutableStateOf("") }
    val pin = remember { mutableStateOf<Marker?>(null) }
    // Train markers survive refreshes so they glide to their new position (like "Red en vivo");
    // the version bump tells the update block to re-add them after a sync.
    val trainMarkers = remember { TrainMarkers() }
    val trainsVersion = remember { mutableIntStateOf(0) }
    val trainIcons = remember { HashMap<Pair<Int, Boolean>, Drawable>() }
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

    // On every new batch of positions: sync the markers, then glide them from where they're drawn
    // to the new spot. A newer batch cancels the glide and starts from the in-between position.
    LaunchedEffect(trains, extraTrains) {
        val spots = (trains.mapIndexed { i, t -> t to "t$i" } + extraTrains.mapIndexed { i, t -> t to "x$i" })
            .map { (t, fallback) ->
                val icon = trainIcons.getOrPut(t.colorArgb to t.headingEast) {
                    buildTrainSprite(ctx, density, t.colorArgb, 30f, flip = !t.headingEast)
                }
                TrainSpot(t.key.ifBlank { fallback }, GeoPoint(t.lat, t.lng), icon, t.label)
            }
            .distinctBy { it.key }
        trainMarkers.sync(mapView, spots)
        trainsVersion.intValue++
        if (spots.isEmpty()) return@LaunchedEffect
        val glide = Animatable(0f)
        glide.animateTo(1f, tween(TRAIN_GLIDE_MS, easing = FastOutSlowInEasing)) {
            trainMarkers.step(value)
            mapView.invalidate()
        }
        trainMarkers.settle()
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
                append(lat).append(',').append(lng).append('#').append(label).append('#').append(lineColorArgb)
                lineStations.forEach { append(';').append(it.id) }
            }
            val stationsChanged = key != stationsKey.value
            if (stationsChanged) {
                stationsKey.value = key
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
                    stops += LineStop(marker, plain, labeled, point, if (focused) 0 else if (terminal) 1 else 2)
                }
                pin.value = Marker(mv).apply {
                    position = GeoPoint(lat, lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = label
                    setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                }
            }

            // Draw order: stations, then the pin, then the trains on top. Same marker instances
            // every time, so re-adding them doesn't blink.
            mv.overlays.clear()
            stops.forEach { mv.overlays.add(it.marker) }
            pin.value?.let { mv.overlays.add(it) }
            trainsVersion.intValue  // re-run after each marker sync
            mv.overlays.addAll(trainMarkers.markers)
            if (stationsChanged) declutterLabels(mv, stops)
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
