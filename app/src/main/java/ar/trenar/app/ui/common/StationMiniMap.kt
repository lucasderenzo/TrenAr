package ar.trenar.app.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ar.trenar.app.R
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
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

/**
 * A small OpenStreetMap view centered on a station, with a pin at its location and,
 * optionally, live train markers. Fits the view to include the trains when there are any.
 */
@Composable
fun StationMiniMap(
    lat: Double,
    lng: Double,
    label: String,
    modifier: Modifier = Modifier,
    trains: List<MiniTrain> = emptyList(),
) {
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density
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

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            val key = buildString {
                append(lat).append(',').append(lng)
                trains.forEach { append('|').append(it.lat).append(',').append(it.lng) }
            }
            if (key == lastKey.value) return@AndroidView
            lastKey.value = key

            mv.overlays.clear()
            Marker(mv).apply {
                position = GeoPoint(lat, lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = label
                setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
            }.also { mv.overlays.add(it) }

            trains.forEach { t ->
                val icon = trainMarker(ctx, density, t.colorArgb, t.headingEast)
                Marker(mv).apply {
                    position = GeoPoint(t.lat, t.lng)
                    this.icon = icon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = t.label
                    setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                }.also { mv.overlays.add(it) }
            }
            mv.invalidate()

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

/** A side-view train sprite tinted in the line color with a white outline; [flip] mirrors it. */
private fun trainMarker(ctx: Context, density: Float, fillArgb: Int, headingEast: Boolean): Drawable {
    val size = (30f * density).toInt().coerceAtLeast(10)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val glyph = ContextCompat.getDrawable(ctx, R.drawable.ic_train_side)?.mutate()
        ?: return BitmapDrawable(ctx.resources, bmp)
    if (!headingEast) {
        canvas.save()
        canvas.scale(-1f, 1f, size / 2f, size / 2f)
    }
    glyph.setTint(AndroidColor.WHITE)
    glyph.setBounds(0, 0, size, size)
    glyph.draw(canvas)
    val inset = (2f * density).toInt()
    glyph.setTint(fillArgb)
    glyph.setBounds(inset, inset, size - inset, size - inset)
    glyph.draw(canvas)
    if (!headingEast) canvas.restore()
    return BitmapDrawable(ctx.resources, bmp)
}
