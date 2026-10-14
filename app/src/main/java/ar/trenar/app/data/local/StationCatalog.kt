package ar.trenar.app.data.local

import android.content.Context
import ar.trenar.app.data.model.StationRef
import ar.trenar.app.util.Geo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.text.Normalizer

@Serializable
private data class CatalogEntry(
    val id: Int,
    val nombre: String,
    val lat: Double,
    val lng: Double,
    val ramales: List<Int> = emptyList(),
    val line: String = "",
)

/** Bundled offline catalog of stations (name + coordinates), loaded once from assets. */
class StationCatalog(private val context: Context) {

    @Volatile
    private var cache: List<StationRef>? = null
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun all(): List<StationRef> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: loadFromAssets().also { cache = it }
        }
    }

    private suspend fun loadFromAssets(): List<StationRef> = withContext(Dispatchers.IO) {
        val text = context.assets.open("stations.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
            .removePrefix("﻿") // tolerate a UTF-8 BOM
        val entries = json.decodeFromString(ListSerializer(CatalogEntry.serializer()), text)
        entries.map { StationRef(it.id, it.nombre, it.lat, it.lng, it.ramales, it.line) }
    }

    suspend fun nearest(lat: Double, lng: Double, limit: Int = 8): List<StationRef> =
        all().map { it.copy(distanceMeters = Geo.haversine(lat, lng, it.lat, it.lng)) }
            .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
            .take(limit)

    /**
     * All stations within [radiusMeters] (pass Int.MAX_VALUE for no limit), nearest first, capped at
     * [cap]. If nothing falls inside the radius, returns the single closest station so it's never empty.
     */
    suspend fun nearestInRadius(
        lat: Double,
        lng: Double,
        radiusMeters: Int = 1000,
        cap: Int = 20,
    ): List<StationRef> {
        val withDist = all()
            .map { it.copy(distanceMeters = Geo.haversine(lat, lng, it.lat, it.lng)) }
            .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
        val inRadius = withDist.filter { (it.distanceMeters ?: Double.MAX_VALUE) <= radiusMeters.toDouble() }
        return inRadius.ifEmpty { withDist.take(1) }.take(cap)
    }

    suspend fun search(query: String): List<StationRef> {
        val q = normalize(query)
        if (q.isBlank()) return emptyList()
        return all()
            .filter { normalize(it.name).contains(q) }
            .sortedWith(compareByDescending<StationRef> { normalize(it.name).startsWith(q) }
                .thenBy { it.name })
            .take(40)
    }

    suspend fun byId(id: Int): StationRef? = all().firstOrNull { it.id == id }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
