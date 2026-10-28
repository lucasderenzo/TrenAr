package ar.trenar.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ar.trenar.app.data.model.ManualLocation
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
    private val manualLatKey = doublePreferencesKey("manual_location_lat")
    private val manualLngKey = doublePreferencesKey("manual_location_lng")
    private val manualAddressKey = stringPreferencesKey("manual_location_address")

    val favorites: Flow<List<Int>> = ds.data.map { p ->
        (p[favoritesKey] ?: emptySet()).mapNotNull { it.toIntOrNull() }.sorted()
    }

    /** Favorite Ecobici stations (GBFS station ids, which are strings). */
    val bikeFavorites: Flow<Set<String>> = ds.data.map { it[bikeFavoritesKey] ?: emptySet() }

    val pinnedStation: Flow<Int?> = ds.data.map { it[pinnedKey] }

    val notifyEnabled: Flow<Boolean> = ds.data.map { it[notifyKey] ?: true }

    /** Search radius for Cercanas, in meters. Default 1000. Int.MAX_VALUE = no radius. */
    val nearbyRadiusMeters: Flow<Int> = ds.data.map { p ->
        // keep only the supported options; map any older/stale value to the default
        p[radiusKey]?.takeIf { it == 1000 || it == 3000 || it == Int.MAX_VALUE } ?: 1000
    }

    /** Manually entered location for Cercanas (replaces GPS when set), or null when using GPS. */
    val manualLocation: Flow<ManualLocation?> = ds.data.map { p ->
        val lat = p[manualLatKey]
        val lng = p[manualLngKey]
        val address = p[manualAddressKey]
        if (lat != null && lng != null && address != null) ManualLocation(lat, lng, address) else null
    }

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

    suspend fun setManualLocation(lat: Double, lng: Double, address: String) {
        ds.edit { p ->
            p[manualLatKey] = lat
            p[manualLngKey] = lng
            p[manualAddressKey] = address
        }
    }

    suspend fun clearManualLocation() {
        ds.edit { p ->
            p.remove(manualLatKey)
            p.remove(manualLngKey)
            p.remove(manualAddressKey)
        }
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
    suspend fun manualLocationNow(): ManualLocation? = manualLocation.first()
}
