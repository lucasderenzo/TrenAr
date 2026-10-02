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
)

class CercanasViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val location = ServiceLocator.location

    private val _state = MutableStateFlow(CercanasUiState())
    val state = _state.asStateFlow()

    private var lastLoc: Location? = null

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
                when (mode) {
                    CercanasMode.TREN -> {
                        val near = repo.nearestStations(loc.latitude, loc.longitude, 8)
                        _state.update {
                            it.copy(items = near.map { s -> StationWithNext(s, null, loading = true) }, loading = false)
                        }
                        val enriched = repo.withNextArrivals(near)
                        if (_state.value.mode == CercanasMode.TREN) {
                            _state.update { it.copy(items = enriched) }
                        }
                    }
                    CercanasMode.ECOBICI -> {
                        val bikes = repo.nearestBikes(loc.latitude, loc.longitude, 12)
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
