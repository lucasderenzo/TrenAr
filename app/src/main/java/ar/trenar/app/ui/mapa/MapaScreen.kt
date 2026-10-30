package ar.trenar.app.ui.mapa

import android.Manifest
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.trenar.app.R
import ar.trenar.app.data.LineInfo
import ar.trenar.app.data.LineTerminals
import ar.trenar.app.di.ServiceLocator
import ar.trenar.app.ui.common.LineBadge
import ar.trenar.app.ui.common.LineStop
import ar.trenar.app.ui.common.buildMarker
import ar.trenar.app.ui.common.buildStationMarker
import ar.trenar.app.ui.common.buildTrainSprite
import ar.trenar.app.ui.common.declutterLabels
import ar.trenar.app.ui.common.TabHeader
import ar.trenar.app.ui.theme.LineColors
import ar.trenar.app.ui.theme.OnTimeGreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private const val ECOBICI_YELLOW = "#F2B705"
private const val LOCATION_BLUE = "#1E88E5"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapaScreen(
    onOpenStation: (Int) -> Unit,
    viewModel: MapaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density
    val scope = rememberCoroutineScope()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val myLocation = remember { mutableStateOf<GeoPoint?>(null) }

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(10.5)
            controller.setCenter(GeoPoint(-34.61, -58.42))
        }
    }
    val lastFitted = remember { mutableStateOf("") }
    // Station markers are rebuilt only when the line or its stations change, not on every train
    // refresh, so the zoom-based name declutter only has to run on zoom and on station changes.
    val stops = remember { mutableListOf<LineStop>() }
    val stopsKey = remember { mutableStateOf("") }

    DisposableEffect(Unit) {
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

    val locate = {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        } else {
            scope.launch {
                val loc = runCatching { ServiceLocator.location.current() }.getOrNull()
                if (loc != null) {
                    val gp = GeoPoint(loc.latitude, loc.longitude)
                    myLocation.value = gp
                    mapView.controller.animateTo(gp)
                    mapView.controller.setZoom(15.0)
                }
            }
            Unit
        }
    }

    Column(Modifier.fillMaxSize()) {
        TabHeader(stringResource(R.string.red_en_vivo))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.lines) { line ->
                FilterChip(
                    selected = state.selected == line,
                    onClick = { viewModel.select(line) },
                    label = { Text(line) },
                    leadingIcon = { LineBadge(line = line, size = 24.dp, corner = 7.dp) },
                )
            }
            item {
                FilterChip(
                    selected = state.isBikes,
                    onClick = { viewModel.select(MapaViewModel.ECOBICI) },
                    label = { Text(stringResource(R.string.ecobici)) },
                    leadingIcon = {
                        Icon(Icons.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                )
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    mv.overlays.clear()
                    if (state.isBikes) {
                        stops.clear()
                        stopsKey.value = ""
                        val bikeIcon = buildMarker(
                            ctx, density, AndroidColor.parseColor(ECOBICI_YELLOW), 20f,
                            AndroidColor.WHITE, 2f, R.drawable.ic_bike,
                        )
                        state.bikes.forEach { b ->
                            Marker(mv).apply {
                                position = GeoPoint(b.lat, b.lng)
                                icon = bikeIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = ctx.getString(R.string.bike_info, b.name, b.bikes, b.docks)
                                setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                            }.also { mv.overlays.add(it) }
                        }
                    } else {
                        val argb = LineColors.colorFor(state.selected).toArgb()
                        val key = buildString {
                            append(state.selected).append('#').append(argb)
                            state.stations.forEach { append(';').append(it.id) }
                        }
                        val stationsChanged = key != stopsKey.value
                        if (stationsChanged) {
                            stopsKey.value = key
                            stops.clear()
                            // terminals: double-size circle with "Est. Terminal" above and the name below,
                            // always labeled; other names show up as the zoom makes room for them without overlapping.
                            val terminalTag = ctx.getString(R.string.est_terminal)
                            state.stations.forEach { st ->
                                val terminal = LineTerminals.isTerminal(state.selected, st.name)
                                val sizeDp = if (terminal) 36f else 18f
                                val stroke = if (terminal) 2f else 1.5f
                                val plain = buildStationMarker(
                                    ctx, density, argb, sizeDp, AndroidColor.WHITE, stroke, R.drawable.ic_stat_train,
                                    null,
                                )
                                val labeled = buildStationMarker(
                                    ctx, density, argb, sizeDp, AndroidColor.WHITE, stroke, R.drawable.ic_stat_train,
                                    st.name, if (terminal) terminalTag else null,
                                )
                                val point = GeoPoint(st.lat, st.lng)
                                val marker = Marker(mv).apply {
                                    position = point
                                    icon = plain.drawable
                                    setAnchor(Marker.ANCHOR_CENTER, plain.anchorY)
                                    title = st.name
                                    setOnMarkerClickListener { _, _ -> onOpenStation(st.id); true }
                                }
                                stops += LineStop(marker, plain, labeled, point, if (terminal) 0 else 1, pinned = terminal)
                            }
                        }
                        stops.forEach { mv.overlays.add(it.marker) }
                        if (stationsChanged) declutterLabels(mv, stops)
                        // a 3D-ish side-view train sprite (line-colored, white outline), one per heading
                        val trainEast = buildTrainSprite(ctx, density, argb, 30f, flip = false)
                        val trainWest = buildTrainSprite(ctx, density, argb, 30f, flip = true)
                        state.trains.forEach { t ->
                            val lat = t.trainLat ?: return@forEach
                            val lng = t.trainLng ?: return@forEach
                            // Orient by where the destination terminal sits relative to the train.
                            val destLng = state.stations.firstOrNull { it.name == t.destination }?.lng
                            val east = destLng?.let { it >= lng } ?: true
                            Marker(mv).apply {
                                position = GeoPoint(lat, lng)
                                icon = if (east) trainEast else trainWest
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = ctx.getString(R.string.tren_a, t.destination)
                                setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                            }.also { mv.overlays.add(it) }
                        }
                        // schedule-estimated positions for the no-realtime lines
                        state.estimatedTrains.forEach { t ->
                            Marker(mv).apply {
                                position = GeoPoint(t.lat, t.lng)
                                icon = if (t.headingEast) trainEast else trainWest
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = ctx.getString(R.string.tren_estimado_a, t.destination)
                                setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                            }.also { mv.overlays.add(it) }
                        }
                    }
                    myLocation.value?.let { gp ->
                        val personIcon = buildMarker(
                            ctx, density, AndroidColor.parseColor(LOCATION_BLUE), 26f,
                            AndroidColor.WHITE, 3f, R.drawable.ic_person,
                        )
                        Marker(mv).apply {
                            position = gp
                            icon = personIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = ctx.getString(R.string.estas_aca)
                            setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                        }.also { mv.overlays.add(it) }
                    }
                    mv.invalidate()

                    if (state.selected != lastFitted.value) {
                        val pts = if (state.isBikes) state.bikes.map { GeoPoint(it.lat, it.lng) }
                        else state.stations.map { GeoPoint(it.lat, it.lng) }
                        if (pts.isNotEmpty()) {
                            val box = BoundingBox.fromGeoPointsSafe(pts)
                            mv.post { runCatching { mv.zoomToBoundingBox(box, false, 90) } }
                            lastFitted.value = state.selected
                        }
                    }
                },
            )

            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    strokeWidth = 3.dp,
                )
            }

            // Live-count pill (bottom-left)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val hasLive = if (state.isBikes) state.bikes.isNotEmpty() else state.trains.isNotEmpty()
                    Spacer(
                        Modifier.size(8.dp).background(
                            if (hasLive) OnTimeGreen else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = when {
                            state.isBikes -> stringResource(R.string.pill_ecobici, state.bikes.size)
                            !LineInfo.hasRealtime(state.selected) -> stringResource(R.string.pill_sin_vivo, state.selected, state.stations.size)
                            else -> stringResource(R.string.pill_en_vivo, state.selected, state.trains.size)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Location + zoom buttons (bottom-right, same height as the pill)
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZoomButton(Icons.Filled.MyLocation, stringResource(R.string.mi_ubicacion)) { locate() }
                ZoomButton(Icons.Filled.Add, stringResource(R.string.acercar)) { mapView.controller.zoomIn() }
                ZoomButton(Icons.Filled.Remove, stringResource(R.string.alejar)) { mapView.controller.zoomOut() }
            }
        }
    }
}

@Composable
private fun ZoomButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.size(44.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = desc, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

