package ar.trenar.app.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.model.ServiceAlert
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertasUiState(
    val loading: Boolean = true,
    val alerts: List<ServiceAlert> = emptyList(),
    val error: String? = null,
)

class AlertasViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val _state = MutableStateFlow(AlertasUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val alerts = repo.alerts()
                _state.update { it.copy(loading = false, alerts = alerts, error = null) }
            } catch (e: Exception) {
                android.util.Log.e("TrenAlertas", "load failed", e)
                _state.update { it.copy(loading = false, error = "No pudimos cargar las alertas.") }
            }
        }
    }
}
