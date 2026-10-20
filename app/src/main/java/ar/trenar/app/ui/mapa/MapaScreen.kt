package ar.trenar.app.ui.mapa

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.trenar.app.R
import ar.trenar.app.data.LineInfo
import ar.trenar.app.data.LineTerminals
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
                        state.stations.forEach { st ->
                            val label = if (LineTerminals.isTerminal(state.selected, st.name)) st.name else null
                            val art = buildStationMarker(
                                ctx, density, argb, 18f, AndroidColor.WHITE, 1.5f, R.drawable.ic_stat_train, label,
                            )
                            Marker(mv).apply {
                                position = GeoPoint(st.lat, st.lng)
                                icon = art.drawable
                                setAnchor(Marker.ANCHOR_CENTER, art.anchorY)
                                title = st.name
                                setOnMarkerClickListener { _, _ -> onOpenStation(st.id); true }
                            }.also { mv.overlays.add(it) }
                        }
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

/** A marker drawable together with the vertical anchor that keeps its hotspot on the geo point. */
private class MarkerArt(val drawable: Drawable, val anchorY: Float)

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
    drawCircleGlyph(canvas, ctx, size / 2f, size / 2f, size, ringW, fill, ringColor, glyphRes)
    return BitmapDrawable(ctx.resources, bmp)
}

/** Draws a filled circle + ring + centered white glyph onto [canvas] at ([cx],[cy]). */
private fun drawCircleGlyph(
    canvas: Canvas,
    ctx: Context,
    cx: Float,
    cy: Float,
    size: Int,
    ringW: Float,
    fill: Int,
    ringColor: Int,
    glyphRes: Int,
) {
    val r = size / 2f
    canvas.drawCircle(cx, cy, r - ringW, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
    canvas.drawCircle(
        cx, cy, r - ringW / 2f - 0.5f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            style = Paint.Style.STROKE
            strokeWidth = ringW
        },
    )
    ContextCompat.getDrawable(ctx, glyphRes)?.mutate()?.let { glyph ->
        glyph.setTint(AndroidColor.WHITE)
        val inset = (ringW + size * 0.2f)
        glyph.setBounds(
            (cx - r + inset).toInt(), (cy - r + inset).toInt(),
            (cx + r - inset).toInt(), (cy + r - inset).toInt(),
        )
        glyph.draw(canvas)
    }
}

/**
 * A circular station marker; when [label] is set (terminal stations), the name is drawn in bold
 * underneath the circle with a white halo so it stays readable over the map tiles.
 */
private fun buildStationMarker(
    ctx: Context,
    density: Float,
    fill: Int,
    sizeDp: Float,
    ringColor: Int,
    ringWidthDp: Float,
    glyphRes: Int,
    label: String?,
): MarkerArt {
    val circle = (sizeDp * density).toInt().coerceAtLeast(8)
    val ringW = ringWidthDp * density
    if (label == null) {
        val bmp = Bitmap.createBitmap(circle, circle, Bitmap.Config.ARGB_8888)
        drawCircleGlyph(Canvas(bmp), ctx, circle / 2f, circle / 2f, circle, ringW, fill, ringColor, glyphRes)
        return MarkerArt(BitmapDrawable(ctx.resources, bmp), 0.5f)
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        textSize = 11f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val haloPaint = Paint(textPaint).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = AndroidColor.WHITE
    }
    val fm = textPaint.fontMetrics
    val textH = fm.descent - fm.ascent
    val gap = 2f * density
    val textW = textPaint.measureText(label)
    val w = maxOf(circle.toFloat(), textW + 6f * density).toInt()
    val h = (circle + gap + textH).toInt() + 1
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = w / 2f
    drawCircleGlyph(canvas, ctx, cx, circle / 2f, circle, ringW, fill, ringColor, glyphRes)
    val baseY = circle + gap - fm.ascent
    canvas.drawText(label, cx, baseY, haloPaint)
    canvas.drawText(label, cx, baseY, textPaint)
    return MarkerArt(BitmapDrawable(ctx.resources, bmp), (circle / 2f) / h)
}

/**
 * A side-view train sprite tinted in the line color with a white outline (no badge). The base
 * glyph faces right (eastbound); [flip] mirrors it horizontally so the nose points left for
 * westbound trains, matching the real direction of travel.
 */
private fun buildTrainSprite(ctx: Context, density: Float, fillArgb: Int, sizeDp: Float, flip: Boolean): Drawable {
    val size = (sizeDp * density).toInt().coerceAtLeast(10)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val glyph = ContextCompat.getDrawable(ctx, R.drawable.ic_train_side)?.mutate()
        ?: return BitmapDrawable(ctx.resources, bmp)
    if (flip) {
        canvas.save()
        canvas.scale(-1f, 1f, size / 2f, size / 2f)
    }
    // white outline (full size) then the colored body slightly inset
    glyph.setTint(AndroidColor.WHITE)
    glyph.setBounds(0, 0, size, size)
    glyph.draw(canvas)
    val inset = (2f * density).toInt()
    glyph.setTint(fillArgb)
    glyph.setBounds(inset, inset, size - inset, size - inset)
    glyph.draw(canvas)
    if (flip) canvas.restore()
    return BitmapDrawable(ctx.resources, bmp)
}
