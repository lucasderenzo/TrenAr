package ar.trenar.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.model.StationRef
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val repo = ServiceLocator.repository

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _results = MutableStateFlow<List<StationRef>>(emptyList())
    val results = _results.asStateFlow()

    @OptIn(FlowPreview::class)
    private val debounced = _query.debounce(140).distinctUntilChanged()

    init {
        viewModelScope.launch {
            debounced.collect { q ->
                _results.value = if (q.isBlank()) emptyList() else repo.searchStations(q)
            }
        }
    }

    fun onQueryChange(q: String) {
        _query.value = q
    }
}
