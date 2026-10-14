package ar.trenar.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens walking directions to a point in Google Maps (or the browser as a fallback). */
object MapsIntent {
    fun directionsTo(context: Context, lat: Double, lng: Double, label: String? = null) {
        val dest = "$lat,$lng"
        // Prefer the Google Maps app with walking mode; fall back to the universal maps URL.
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=$dest&travelmode=walking",
        )
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
