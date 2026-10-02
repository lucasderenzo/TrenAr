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
    val selected: String = "",          // a line name, or ECOBICI
    val stations: List<StationRef> = emptyList(),
    val trains: List<Arrival> = emptyList(),
    val bikes: List<BikeStation> = emptyList(),
    val loading: Boolean = false,
) {
    val isBikes: Boolean get() = selected == MapaViewModel.ECOBICI
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

    fun refresh() {
        val sel = _state.value.selected
        if (sel == ECOBICI) loadBikes(force = true) else if (sel.isNotBlank()) loadLine(sel)
    }

    private fun loadLine(line: String) {
        viewModelScope.launch {
            val stations = repo.stationsOfLine(line)
            _state.update { it.copy(stations = stations, trains = emptyList(), loading = true) }
            val trains = runCatching { repo.liveTrains(line) }.getOrDefault(emptyList())
            _state.update {
                if (it.selected == line) it.copy(trains = trains, loading = false)
                else it.copy(loading = false)
            }
        }
    }

    private fun loadBikes(force: Boolean = false) {
        viewModelScope.launch {
            if (!force && _state.value.bikes.isNotEmpty()) return@launch
            _state.update { it.copy(loading = true) }
            val bikes = runCatching { repo.ecobiciStations() }.getOrDefault(emptyList())
            _state.update {
                if (it.selected == ECOBICI) it.copy(bikes = bikes, loading = false)
                else it.copy(bikes = bikes, loading = false)
            }
        }
    }

    companion object {
        const val ECOBICI = "Ecobici"
    }
}
