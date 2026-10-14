package ar.trenar.app.ui.cercanas

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.model.BikeNearby
import ar.trenar.app.data.model.StationWithNext
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CercanasMode { TREN, ECOBICI }

data class CercanasUiState(
    val mode: CercanasMode = CercanasMode.TREN,
    val items: List<StationWithNext> = emptyList(),
    val bikeItems: List<BikeNearby> = emptyList(),
    val loading: Boolean = false,
    val locationDenied: Boolean = false,
    val locationUnavailable: Boolean = false,
    val radiusMeters: Int = 1000,
    val bikeFavorites: Set<String> = emptySet(),
)

class CercanasViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val location = ServiceLocator.location
    private val prefs = ServiceLocator.prefs

    private val _state = MutableStateFlow(CercanasUiState())
    val state = _state.asStateFlow()

    private var lastLoc: Location? = null

    init {
        viewModelScope.launch {
            prefs.nearbyRadiusMeters.collect { r -> _state.update { it.copy(radiusMeters = r) } }
        }
        viewModelScope.launch {
            prefs.bikeFavorites.collect { f -> _state.update { it.copy(bikeFavorites = f) } }
        }
    }

    fun refresh() = load(_state.value.mode, force = true)

    fun setMode(mode: CercanasMode) {
        if (mode == _state.value.mode) return
        _state.update { it.copy(mode = mode) }
        val needs = when (mode) {
            CercanasMode.TREN -> _state.value.items.isEmpty()
            CercanasMode.ECOBICI -> _state.value.bikeItems.isEmpty()
        }
        if (needs) load(mode, force = false) else _state.update { it.copy(loading = false) }
    }

    fun setRadius(meters: Int) {
        if (meters == _state.value.radiusMeters) return
        viewModelScope.launch { prefs.setNearbyRadius(meters) }
        _state.update { it.copy(radiusMeters = meters) }
        load(_state.value.mode, force = true)
    }

    fun toggleBikeFavorite(id: String) {
        viewModelScope.launch { prefs.toggleBikeFavorite(id) }
    }

    private fun load(mode: CercanasMode, force: Boolean) {
        viewModelScope.launch {
            if (!location.hasPermission()) {
                _state.update { it.copy(locationDenied = true, loading = false) }
                return@launch
            }
            _state.update { it.copy(loading = true, locationDenied = false, locationUnavailable = false) }
            try {
                val loc = (if (force) null else lastLoc) ?: location.current()?.also { lastLoc = it }
                if (loc == null) {
                    _state.update { it.copy(loading = false, locationUnavailable = true) }
                    return@launch
                }
                val radius = _state.value.radiusMeters
                when (mode) {
                    CercanasMode.TREN -> {
                        val near = repo.nearestStationsInRadius(loc.latitude, loc.longitude, radius, cap = 20)
                        _state.update {
                            it.copy(items = near.map { s -> StationWithNext(s, null, loading = true) }, loading = false)
                        }
                        val enriched = repo.withNextArrivals(near)
                        if (_state.value.mode == CercanasMode.TREN) {
                            _state.update { it.copy(items = enriched) }
                        }
                    }
                    CercanasMode.ECOBICI -> {
                        val bikes = repo.nearestBikes(loc.latitude, loc.longitude, radius, limit = 20)
                        _state.update { it.copy(bikeItems = bikes, loading = false) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TrenCercanas", "load failed", e)
                _state.update { it.copy(loading = false, locationUnavailable = true) }
            }
        }
    }
}
