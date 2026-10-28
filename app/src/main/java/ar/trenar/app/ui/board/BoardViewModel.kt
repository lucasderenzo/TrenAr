package ar.trenar.app.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ar.trenar.app.data.model.Arrival
import ar.trenar.app.data.model.BoardState
import ar.trenar.app.data.model.EstimatedTrain
import ar.trenar.app.data.model.StationRef
import ar.trenar.app.data.LineInfo
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
    val distanceMeters: Double? = null,
    val filterDestination: String? = null,
    /** Every station of this station's line, drawn on the mini-map (like "Red en vivo"). */
    val lineStations: List<StationRef> = emptyList(),
    /** Live trains on the whole line (realtime lines), for the mini-map when zoomed out. */
    val lineTrains: List<Arrival> = emptyList(),
    /** Schedule-estimated trains on the whole line (no-realtime lines). */
    val lineEstimated: List<EstimatedTrain> = emptyList(),
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
    private var lineTrainsLoadedAt = 0L
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
            _state.update { it.copy(walkSeconds = (meters / 1.35).toInt(), distanceMeters = meters) }
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

    /** [auto] marks the periodic resync, which refreshes the whole-line trains less often. */
    fun load(initial: Boolean = false, auto: Boolean = false) {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = initial && it.board == null, refreshing = !initial, error = null)
            }
            try {
                val board = repo.board(stationId, cantidad = 24)
                _state.update { it.copy(loading = false, refreshing = false, board = board, error = null) }
                board.station?.line?.takeIf { it.isNotBlank() }?.let { loadLine(it, force = !initial && !auto) }
            } catch (e: Exception) {
                android.util.Log.e("TrenBoard", "board load failed: ${e.javaClass.simpleName}: ${e.message}", e)
                _state.update {
                    it.copy(loading = false, refreshing = false, error = friendly(e))
                }
            }
        }
    }

    /**
     * Loads the rest of the line for the mini-map, in the background so the board isn't delayed.
     * Live trains of the whole line take several requests, so they're refreshed at most once a
     * minute by the periodic resync (or on a manual refresh).
     */
    private fun loadLine(line: String, force: Boolean) {
        viewModelScope.launch {
            if (_state.value.lineStations.isEmpty()) {
                val stations = runCatching { repo.stationsOfLine(line) }.getOrDefault(emptyList())
                _state.update { it.copy(lineStations = stations) }
            }
            val now = System.currentTimeMillis() / 1000
            if (!force && now - lineTrainsLoadedAt < LINE_TRAINS_MIN_INTERVAL_SEC) return@launch
            lineTrainsLoadedAt = now
            if (LineInfo.hasRealtime(line)) {
                val trains = runCatching { repo.liveTrains(line) }.getOrNull() ?: return@launch
                _state.update { it.copy(lineTrains = trains) }
            } else {
                val est = runCatching { repo.estimatedTrains(line) }.getOrDefault(emptyList())
                _state.update { it.copy(lineEstimated = est) }
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

    private fun friendly(e: Exception): String {
        val c = ServiceLocator.appContext
        return when (e) {
            is java.net.UnknownHostException -> c.getString(ar.trenar.app.R.string.error_sin_conexion)
            is java.net.SocketTimeoutException -> c.getString(ar.trenar.app.R.string.error_timeout)
            else -> c.getString(ar.trenar.app.R.string.error_generico)
        }
    }

    companion object {
        private const val LINE_TRAINS_MIN_INTERVAL_SEC = 60L

        fun factory(stationId: Int) = viewModelFactory {
            initializer { BoardViewModel(stationId) }
        }
    }
}
