package ar.trenar.app.ui.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.model.Arrival
import ar.trenar.app.data.model.BikeStation
import ar.trenar.app.data.model.StationRef
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapaUiState(
    val lines: List<String> = emptyList(),
    val selectedLine: String = "",
    val stations: List<StationRef> = emptyList(),
    val trains: List<Arrival> = emptyList(),
    val loadingTrains: Boolean = false,
    val showBikes: Boolean = false,
    val bikes: List<BikeStation> = emptyList(),
    val loadingBikes: Boolean = false,
)

class MapaViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val _state = MutableStateFlow(MapaUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lines = repo.availableLines()
            val first = lines.firstOrNull() ?: ""
            _state.update { it.copy(lines = lines, selectedLine = first) }
            if (first.isNotBlank()) loadLine(first)
        }
    }

    fun selectLine(line: String) {
        if (line == _state.value.selectedLine && _state.value.stations.isNotEmpty()) return
        _state.update { it.copy(selectedLine = line) }
        loadLine(line)
    }

    fun refreshTrains() {
        val line = _state.value.selectedLine
        if (line.isNotBlank()) loadTrains(line)
    }

    fun toggleBikes() {
        val showing = !_state.value.showBikes
        _state.update { it.copy(showBikes = showing) }
        if (showing && _state.value.bikes.isEmpty()) {
            viewModelScope.launch {
                _state.update { it.copy(loadingBikes = true) }
                val bikes = runCatching { repo.ecobiciStations() }.getOrDefault(emptyList())
                _state.update { it.copy(bikes = bikes, loadingBikes = false) }
            }
        }
    }

    private fun loadLine(line: String) {
        viewModelScope.launch {
            val stations = repo.stationsOfLine(line)
            _state.update { it.copy(stations = stations, trains = emptyList()) }
            loadTrains(line)
        }
    }

    private fun loadTrains(line: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingTrains = true) }
            val trains = runCatching { repo.liveTrains(line) }.getOrDefault(emptyList())
            _state.update {
                if (it.selectedLine == line) it.copy(trains = trains, loadingTrains = false)
                else it.copy(loadingTrains = false)
            }
        }
    }
}
