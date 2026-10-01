package ar.trenar.app.ui.favoritos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.model.StationWithNext
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritosUiState(
    val items: List<StationWithNext> = emptyList(),
    val loading: Boolean = true,
)

class FavoritosViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val prefs = ServiceLocator.prefs

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
    }
}
