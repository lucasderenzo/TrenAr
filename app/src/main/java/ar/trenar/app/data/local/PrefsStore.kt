package ar.trenar.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "trenar_prefs")

/** User preferences: favorite stations, the widget-pinned station, and notification toggle. */
class PrefsStore(context: Context) {

    private val ds = context.applicationContext.dataStore

    private val favoritesKey = stringSetPreferencesKey("favorites")
    private val bikeFavoritesKey = stringSetPreferencesKey("bike_favorites")
    private val pinnedKey = intPreferencesKey("pinned_station")
    private val notifyKey = booleanPreferencesKey("notify_enabled")
    private val radiusKey = intPreferencesKey("nearby_radius_m")
    private val perLineKey = intPreferencesKey("nearby_per_line")

    val favorites: Flow<List<Int>> = ds.data.map { p ->
        (p[favoritesKey] ?: emptySet()).mapNotNull { it.toIntOrNull() }.sorted()
    }

    /** Favorite Ecobici stations (GBFS station ids, which are strings). */
    val bikeFavorites: Flow<Set<String>> = ds.data.map { it[bikeFavoritesKey] ?: emptySet() }

    val pinnedStation: Flow<Int?> = ds.data.map { it[pinnedKey] }

    val notifyEnabled: Flow<Boolean> = ds.data.map { it[notifyKey] ?: true }

    /** Search radius for Cercanas, in meters. Default 1500. */
    val nearbyRadiusMeters: Flow<Int> = ds.data.map { it[radiusKey] ?: 1500 }

    /** How many nearby stations to show per line (1-3). Default 1. */
    val nearbyPerLine: Flow<Int> = ds.data.map { (it[perLineKey] ?: 1).coerceIn(1, 3) }

    suspend fun toggleFavorite(id: Int) {
        ds.edit { p ->
            val current = (p[favoritesKey] ?: emptySet()).toMutableSet()
            val key = id.toString()
            if (!current.add(key)) current.remove(key)
            p[favoritesKey] = current
        }
    }

    suspend fun toggleBikeFavorite(id: String) {
        ds.edit { p ->
            val current = (p[bikeFavoritesKey] ?: emptySet()).toMutableSet()
            if (!current.add(id)) current.remove(id)
            p[bikeFavoritesKey] = current
        }
    }

    suspend fun setNearbyRadius(meters: Int) {
        ds.edit { it[radiusKey] = meters }
    }

    suspend fun setNearbyPerLine(count: Int) {
        ds.edit { it[perLineKey] = count.coerceIn(1, 3) }
    }

    suspend fun setPinned(id: Int) {
        ds.edit { it[pinnedKey] = id }
    }

    suspend fun setNotifyEnabled(enabled: Boolean) {
        ds.edit { it[notifyKey] = enabled }
    }

    suspend fun favoritesNow(): List<Int> = favorites.first()
    suspend fun bikeFavoritesNow(): Set<String> = bikeFavorites.first()
    suspend fun pinnedNow(): Int? = pinnedStation.first()
    suspend fun notifyEnabledNow(): Boolean = notifyEnabled.first()
    suspend fun nearbyRadiusNow(): Int = nearbyRadiusMeters.first()
    suspend fun nearbyPerLineNow(): Int = nearbyPerLine.first()
}
