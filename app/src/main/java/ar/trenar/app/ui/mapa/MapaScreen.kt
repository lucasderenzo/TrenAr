package ar.trenar.app.ui.mapa

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ar.trenar.app.ui.common.LineBadge
import ar.trenar.app.ui.common.TabHeader
import ar.trenar.app.ui.theme.LineColors
import ar.trenar.app.ui.theme.OnTimeGreen
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapaScreen(
    onOpenStation: (Int) -> Unit,
    viewModel: MapaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
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

    Column(Modifier.fillMaxSize()) {
        TabHeader("Red en vivo")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.lines) { line ->
                FilterChip(
                    selected = state.selectedLine == line,
                    onClick = { viewModel.selectLine(line) },
                    label = { Text(line) },
                    leadingIcon = {
                        LineBadge(line = line, size = 24.dp, corner = 7.dp)
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
                    val argb = LineColors.colorFor(state.selectedLine).toArgb()
                    // Station: small, line-colored, thin white ring.
                    val stationIcon = buildMarker(ctx, density, argb, 18f, AndroidColor.WHITE, 1.5f)
                    // Live train: bigger, with a bright green "en vivo" ring so it clearly stands out.
                    val trainIcon = buildMarker(ctx, density, argb, 34f, OnTimeGreen.toArgb(), 3.5f)

                    state.stations.forEach { st ->
                        val marker = Marker(mv).apply {
                            position = GeoPoint(st.lat, st.lng)
                            icon = stationIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = st.name
                            setOnMarkerClickListener { _, _ ->
                                onOpenStation(st.id); true
                            }
                        }
                        mv.overlays.add(marker)
                    }

                    state.trains.forEach { t ->
                        val lat = t.trainLat ?: return@forEach
                        val lng = t.trainLng ?: return@forEach
                        val marker = Marker(mv).apply {
                            position = GeoPoint(lat, lng)
                            icon = trainIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Tren a ${t.destination}"
                            setOnMarkerClickListener { m, _ ->
                                m.showInfoWindow(); true
                            }
                        }
                        mv.overlays.add(marker)
                    }
                    mv.invalidate()

                    if (state.selectedLine != lastFitted.value && state.stations.isNotEmpty()) {
                        val pts = state.stations.map { GeoPoint(it.lat, it.lng) }
                        val box = BoundingBox.fromGeoPointsSafe(pts)
                        mv.post { runCatching { mv.zoomToBoundingBox(box, false, 90) } }
                        lastFitted.value = state.selectedLine
                    }
                },
            )

            if (state.loadingTrains) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    strokeWidth = 3.dp,
                )
            }

            // Live-count pill, foto-2 style
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(
                        Modifier.size(8.dp).background(
                            if (state.trains.isNotEmpty()) OnTimeGreen else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "${state.selectedLine} · ${state.trains.size} en vivo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** A circular marker: [fill] core + a [ringColor] ring + a white train glyph. */
private fun buildMarker(
    ctx: Context,
    density: Float,
    fill: Int,
    sizeDp: Float,
    ringColor: Int,
    ringWidthDp: Float,
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
    val train = ContextCompat.getDrawable(ctx, R.drawable.ic_stat_train)?.mutate()
    if (train != null) {
        train.setTint(AndroidColor.WHITE)
        val inset = (ringW + size * 0.2f).toInt()
        train.setBounds(inset, inset, size - inset, size - inset)
        train.draw(canvas)
    }
    return BitmapDrawable(ctx.resources, bmp)
}
