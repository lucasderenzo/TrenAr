package ar.trenar.app.ui.favoritos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.model.BikeNearby
import ar.trenar.app.data.model.StationWithNext
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritosUiState(
    val items: List<StationWithNext> = emptyList(),
    val bikeItems: List<BikeNearby> = emptyList(),
    val bikeFavorites: Set<String> = emptySet(),
    val loading: Boolean = true,
)

class FavoritosViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val prefs = ServiceLocator.prefs
    private val location = ServiceLocator.location

    private val _state = MutableStateFlow(FavoritosUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.favorites.collect { ids ->
                val stations = runCatching { ids.mapNotNull { repo.station(it) } }.getOrDefault(emptyList())
                if (stations.isEmpty()) {
                    _state.update { it.copy(items = emptyList(), loading = false) }
                    return@collect
                }
                _state.update {
                    it.copy(items = stations.map { st -> StationWithNext(st, null, loading = true) }, loading = false)
                }
                val enriched = runCatching { repo.withNextArrivals(stations) }.getOrDefault(
                    stations.map { st -> StationWithNext(st, null) },
                )
                _state.update { it.copy(items = enriched) }
            }
        }
        viewModelScope.launch {
            prefs.bikeFavorites.collect { ids ->
                _state.update { it.copy(bikeFavorites = ids) }
                if (ids.isEmpty()) {
                    _state.update { it.copy(bikeItems = emptyList()) }
                    return@collect
                }
                val loc = runCatching {
                    if (location.hasPermission()) location.current() else null
                }.getOrNull()
                val bikes = runCatching {
                    repo.favoriteBikes(ids, loc?.latitude, loc?.longitude)
                }.getOrDefault(emptyList())
                _state.update { it.copy(bikeItems = bikes) }
            }
        }
    }

    fun toggleBikeFavorite(id: String) {
        viewModelScope.launch { prefs.toggleBikeFavorite(id) }
    }
}
