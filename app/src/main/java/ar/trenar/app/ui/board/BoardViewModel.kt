package ar.trenar.app.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ar.trenar.app.data.model.Arrival
import ar.trenar.app.data.model.BoardState
import ar.trenar.app.di.ServiceLocator
import ar.trenar.app.notifications.TrainTrackService
import ar.trenar.app.util.Geo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val board: BoardState? = null,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val isTracking: Boolean = false,
    val walkSeconds: Int? = null,
    val filterDestination: String? = null,
) {
    val destinations: List<String>
        get() = board?.arrivals?.map { it.destination }?.distinct()?.sorted() ?: emptyList()

    val filteredArrivals: List<Arrival>
        get() {
            val all = board?.arrivals ?: emptyList()
            return filterDestination?.let { f -> all.filter { it.destination == f } } ?: all
        }
}

class BoardViewModel(private val stationId: Int) : ViewModel() {

    private val repo = ServiceLocator.repository
    private val prefs = ServiceLocator.prefs

    private val _state = MutableStateFlow(BoardUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.favorites.collect { ids ->
                _state.update { it.copy(isFavorite = ids.contains(stationId)) }
            }
        }
        viewModelScope.launch {
            TrainTrackService.tracked.collect { tracked ->
                _state.update { it.copy(isTracking = tracked == stationId) }
            }
        }
        load(initial = true)
        computeWalkTime()
    }

    private fun computeWalkTime() {
        viewModelScope.launch {
            val location = ServiceLocator.location
            if (!location.hasPermission()) return@launch
            val loc = runCatching { location.current() }.getOrNull() ?: return@launch
            val station = repo.station(stationId) ?: return@launch
            val meters = Geo.haversine(loc.latitude, loc.longitude, station.lat, station.lng)
            // ~1.35 m/s walking pace
            _state.update { it.copy(walkSeconds = (meters / 1.35).toInt()) }
        }
    }

    fun toggleFollow() {
        val ctx = ServiceLocator.appContext
        if (_state.value.isTracking) {
            TrainTrackService.stop(ctx)
        } else {
            val name = _state.value.board?.station?.name ?: "Estación"
            TrainTrackService.start(ctx, stationId, name)
        }
    }

    fun load(initial: Boolean = false) {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = initial && it.board == null, refreshing = !initial, error = null)
            }
            try {
                val board = repo.board(stationId, cantidad = 24)
                _state.update { it.copy(loading = false, refreshing = false, board = board, error = null) }
            } catch (e: Exception) {
                android.util.Log.e("TrenBoard", "board load failed: ${e.javaClass.simpleName}: ${e.message}", e)
                _state.update {
                    it.copy(loading = false, refreshing = false, error = friendly(e))
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            prefs.toggleFavorite(stationId)
            runCatching { ar.trenar.app.widget.WidgetUpdater.updateAll(ServiceLocator.appContext) }
        }
    }

    fun setFilter(destination: String?) {
        _state.update { it.copy(filterDestination = destination) }
    }

    private fun friendly(e: Exception): String = when (e) {
        is java.net.UnknownHostException -> "Sin conexión a internet."
        is java.net.SocketTimeoutException -> "El servicio tardó demasiado en responder."
        else -> "No pudimos obtener los arribos. Probá de nuevo."
    }

    companion object {
        fun factory(stationId: Int) = viewModelFactory {
            initializer { BoardViewModel(stationId) }
        }
    }
}
