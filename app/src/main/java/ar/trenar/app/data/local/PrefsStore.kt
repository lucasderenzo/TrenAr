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
    private val pinnedKey = intPreferencesKey("pinned_station")
    private val notifyKey = booleanPreferencesKey("notify_enabled")

    val favorites: Flow<List<Int>> = ds.data.map { p ->
        (p[favoritesKey] ?: emptySet()).mapNotNull { it.toIntOrNull() }.sorted()
    }

    val pinnedStation: Flow<Int?> = ds.data.map { it[pinnedKey] }

    val notifyEnabled: Flow<Boolean> = ds.data.map { it[notifyKey] ?: true }

    suspend fun toggleFavorite(id: Int) {
        ds.edit { p ->
            val current = (p[favoritesKey] ?: emptySet()).toMutableSet()
            val key = id.toString()
            if (!current.add(key)) current.remove(key)
            p[favoritesKey] = current
        }
    }

    suspend fun setPinned(id: Int) {
        ds.edit { it[pinnedKey] = id }
    }

    suspend fun setNotifyEnabled(enabled: Boolean) {
        ds.edit { it[notifyKey] = enabled }
    }

    suspend fun favoritesNow(): List<Int> = favorites.first()
    suspend fun pinnedNow(): Int? = pinnedStation.first()
    suspend fun notifyEnabledNow(): Boolean = notifyEnabled.first()
}
