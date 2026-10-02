package ar.trenar.app.ui.mapa

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.trenar.app.R
import ar.trenar.app.data.LineInfo
import ar.trenar.app.di.ServiceLocator
import ar.trenar.app.ui.common.LineBadge
import ar.trenar.app.ui.common.TabHeader
import ar.trenar.app.ui.theme.LineColors
import ar.trenar.app.ui.theme.OnTimeGreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
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

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
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
        TabHeader("Red en vivo")
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
                    label = { Text("Ecobici") },
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
                        val bikeIcon = buildMarker(
                            ctx, density, AndroidColor.parseColor(ECOBICI_YELLOW), 20f,
                            AndroidColor.WHITE, 2f, R.drawable.ic_bike,
                        )
                        state.bikes.forEach { b ->
                            Marker(mv).apply {
                                position = GeoPoint(b.lat, b.lng)
                                icon = bikeIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = "${b.name}: ${b.bikes} bicis · ${b.docks} anclajes"
                                setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                            }.also { mv.overlays.add(it) }
                        }
                    } else {
                        val argb = LineColors.colorFor(state.selected).toArgb()
                        val stationIcon = buildMarker(ctx, density, argb, 18f, AndroidColor.WHITE, 1.5f, R.drawable.ic_stat_train)
                        val trainIcon = buildMarker(ctx, density, argb, 34f, OnTimeGreen.toArgb(), 3.5f, R.drawable.ic_stat_train)
                        state.stations.forEach { st ->
                            Marker(mv).apply {
                                position = GeoPoint(st.lat, st.lng)
                                icon = stationIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = st.name
                                setOnMarkerClickListener { _, _ -> onOpenStation(st.id); true }
                            }.also { mv.overlays.add(it) }
                        }
                        state.trains.forEach { t ->
                            val lat = t.trainLat ?: return@forEach
                            val lng = t.trainLng ?: return@forEach
                            Marker(mv).apply {
                                position = GeoPoint(lat, lng)
                                icon = trainIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = "Tren a ${t.destination}"
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
                            title = "Estás acá"
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
                            state.isBikes -> "Ecobici · ${state.bikes.size} estaciones"
                            !LineInfo.hasRealtime(state.selected) ->
                                "${state.selected} · ${state.stations.size} estaciones · sin vivo"
                            else -> "${state.selected} · ${state.trains.size} en vivo"
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
                ZoomButton(Icons.Filled.MyLocation, "Mi ubicación") { locate() }
                ZoomButton(Icons.Filled.Add, "Acercar") { mapView.controller.zoomIn() }
                ZoomButton(Icons.Filled.Remove, "Alejar") { mapView.controller.zoomOut() }
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

/** A circular marker: [fill] core + a [ringColor] ring + a white [glyphRes] glyph. */
private fun buildMarker(
    ctx: Context,
    density: Float,
    fill: Int,
    sizeDp: Float,
    ringColor: Int,
    ringWidthDp: Float,
    glyphRes: Int,
): Drawable {
    val size = (sizeDp * density).toInt().coerceAtLeast(8)
    val ringW = ringWidthDp * density
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val r = size / 2f
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fill }
    canvas.drawCircle(r, r, r - ringW, fillPaint)
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = ringColor
        style = Paint.Style.STROKE
        strokeWidth = ringW
    }
    canvas.drawCircle(r, r, r - ringW / 2f - 0.5f, ringPaint)
    val glyph = ContextCompat.getDrawable(ctx, glyphRes)?.mutate()
    if (glyph != null) {
        glyph.setTint(AndroidColor.WHITE)
        val inset = (ringW + size * 0.2f).toInt()
        glyph.setBounds(inset, inset, size - inset, size - inset)
        glyph.draw(canvas)
    }
    return BitmapDrawable(ctx.resources, bmp)
}
