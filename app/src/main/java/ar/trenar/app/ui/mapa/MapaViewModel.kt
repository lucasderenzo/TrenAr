package ar.trenar.app.ui.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.LineInfo
import ar.trenar.app.data.model.Arrival
import ar.trenar.app.data.model.BikeStation
import ar.trenar.app.data.model.EstimatedTrain
import ar.trenar.app.data.model.StationRef
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapaUiState(
    val lines: List<String> = emptyList(),
    val selected: String = "",          // a line name, or ECOBICI
    val stations: List<StationRef> = emptyList(),
    val trains: List<Arrival> = emptyList(),
    /** Schedule-estimated positions, for no-realtime lines (Urquiza / Belgrano Norte). */
    val estimatedTrains: List<EstimatedTrain> = emptyList(),
    val bikes: List<BikeStation> = emptyList(),
    val loading: Boolean = false,
) {
    val isBikes: Boolean get() = selected == MapaViewModel.ECOBICI
    val isEstimated: Boolean get() = !isBikes && selected.isNotBlank() && !LineInfo.hasRealtime(selected)
}

class MapaViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val _state = MutableStateFlow(MapaUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lines = repo.availableLines()
            val first = lines.firstOrNull() ?: ""
            _state.update { it.copy(lines = lines, selected = first) }
            if (first.isNotBlank()) loadLine(first)
        }
    }

    fun select(chip: String) {
        if (chip == _state.value.selected) return
        _state.update { it.copy(selected = chip) }
        if (chip == ECOBICI) loadBikes() else loadLine(chip)
    }

    /**
     * Silent periodic refresh (called every [AUTO_REFRESH_MS] while the map is visible): keeps the
     * markers on screen while loading, skips if a load is still running, and on a network error
     * leaves the previous positions untouched.
     */
    fun refresh() {
        if (loadJob?.isActive == true) return
        val sel = _state.value.selected
        if (sel == ECOBICI) loadBikes(force = true, silent = true)
        else if (sel.isNotBlank()) loadLine(sel, silent = true)
    }

    private var loadJob: Job? = null

    private fun loadLine(line: String, silent: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!silent) {
                val stations = repo.stationsOfLine(line)
                _state.update {
                    it.copy(stations = stations, trains = emptyList(), estimatedTrains = emptyList(), loading = true)
                }
            }
            if (!LineInfo.hasRealtime(line)) {
                val est = runCatching { repo.estimatedTrains(line) }.getOrNull()
                _state.update {
                    if (it.selected == line && est != null) it.copy(estimatedTrains = est, loading = false)
                    else it.copy(loading = false)
                }
                return@launch
            }
            val trains = runCatching { repo.liveTrains(line) }.getOrNull()
            _state.update {
                if (it.selected == line && trains != null) it.copy(trains = trains, loading = false)
                else it.copy(loading = false)
            }
        }
    }

    private fun loadBikes(force: Boolean = false, silent: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!force && _state.value.bikes.isNotEmpty()) {
                _state.update { it.copy(loading = false) }
                return@launch
            }
            if (!silent) _state.update { it.copy(loading = true) }
            val bikes = runCatching { repo.ecobiciStations() }.getOrNull()
            _state.update {
                if (bikes.isNullOrEmpty()) it.copy(loading = false) else it.copy(bikes = bikes, loading = false)
            }
        }
    }

    companion object {
        const val ECOBICI = "Ecobici"
        const val AUTO_REFRESH_MS = 20_000L
    }
}
