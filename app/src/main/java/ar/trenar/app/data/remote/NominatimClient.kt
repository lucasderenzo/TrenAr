package ar.trenar.app.data.remote

import ar.trenar.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Forward geocoding with OpenStreetMap's Nominatim. Fallback for when the platform Geocoder
 * is missing or broken (devices without Google services, emulators). Only used on an explicit
 * user search, which keeps us well within Nominatim's 1 req/s usage policy.
 */
object NominatimClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** First match for [query] inside the given box, as lat to lng; null when not found/offline. */
    suspend fun search(
        query: String,
        south: Double,
        west: Double,
        north: Double,
        east: Double,
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val url = "https://nominatim.openstreetmap.org/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "jsonv2")
            .addQueryParameter("limit", "1")
            .addQueryParameter("countrycodes", "ar")
            .addQueryParameter("viewbox", "$west,$north,$east,$south")
            .addQueryParameter("bounded", "1")
            .build()
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "TrenAr/${BuildConfig.VERSION_NAME} (ar.trenar.app)")
            .header("Accept-Language", "es")
            .build()
        runCatching {
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use null
                val first = JSONArray(res.body?.string() ?: "[]").optJSONObject(0) ?: return@use null
                first.getString("lat").toDouble() to first.getString("lon").toDouble()
            }
        }.getOrNull()
    }
}
