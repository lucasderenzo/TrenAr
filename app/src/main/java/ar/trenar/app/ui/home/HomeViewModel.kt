package ar.trenar.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.model.StationRef
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val nearby: List<StationRef> = emptyList(),
    val favorites: List<StationRef> = emptyList(),
    val loadingNearby: Boolean = false,
    val locationDenied: Boolean = false,
    val locationUnavailable: Boolean = false,
)

class HomeViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val prefs = ServiceLocator.prefs
    private val location = ServiceLocator.location

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.favorites.collect { ids ->
                val stations = runCatching { ids.mapNotNull { repo.station(it) } }.getOrDefault(emptyList())
                _state.update { it.copy(favorites = stations) }
            }
        }
    }

    fun refreshNearby() {
        viewModelScope.launch {
            if (!location.hasPermission()) {
                _state.update { it.copy(locationDenied = true, loadingNearby = false) }
                return@launch
            }
            _state.update {
                it.copy(loadingNearby = true, locationDenied = false, locationUnavailable = false)
            }
            try {
                val loc = location.current()
                if (loc == null) {
                    _state.update { it.copy(loadingNearby = false, locationUnavailable = true) }
                    return@launch
                }
                val near = repo.nearestStations(loc.latitude, loc.longitude, 8)
                _state.update {
                    it.copy(nearby = near, loadingNearby = false, locationUnavailable = false)
                }
            } catch (e: Exception) {
                android.util.Log.e("TrenHome", "nearby failed", e)
                _state.update { it.copy(loadingNearby = false, locationUnavailable = true) }
            }
        }
    }
}
