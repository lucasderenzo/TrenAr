package ar.trenar.app.ui.cercanas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.model.StationWithNext
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CercanasUiState(
    val items: List<StationWithNext> = emptyList(),
    val loading: Boolean = false,
    val locationDenied: Boolean = false,
    val locationUnavailable: Boolean = false,
)

class CercanasViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val location = ServiceLocator.location

    private val _state = MutableStateFlow(CercanasUiState())
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            if (!location.hasPermission()) {
                _state.update { it.copy(locationDenied = true, loading = false) }
                return@launch
            }
            _state.update { it.copy(loading = true, locationDenied = false, locationUnavailable = false) }
            try {
                val loc = location.current()
                if (loc == null) {
                    _state.update { it.copy(loading = false, locationUnavailable = true) }
                    return@launch
                }
                val near = repo.nearestStations(loc.latitude, loc.longitude, 8)
                // Show stations immediately, then fill arrivals.
                _state.update {
                    it.copy(
                        items = near.map { st -> StationWithNext(st, null, loading = true) },
                        loading = false,
                        locationUnavailable = false,
                    )
                }
                val enriched = repo.withNextArrivals(near)
                _state.update { it.copy(items = enriched) }
            } catch (e: Exception) {
                android.util.Log.e("TrenCercanas", "refresh failed", e)
                _state.update { it.copy(loading = false, locationUnavailable = true) }
            }
        }
    }
}
