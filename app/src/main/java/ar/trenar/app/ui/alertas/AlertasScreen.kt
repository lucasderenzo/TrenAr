package ar.trenar.app.ui.alertas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.trenar.app.data.model.ServiceAlert
import ar.trenar.app.ui.common.EmptyState
import ar.trenar.app.ui.common.TabHeader
import ar.trenar.app.ui.theme.LineColors

@Composable
fun AlertasScreen(viewModel: AlertasViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabHeader("Alertas", modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.load() }, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                }
            }
        }

        when {
            state.loading -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> item {
                EmptyState(
                    icon = Icons.Filled.Refresh,
                    title = "No se pudo cargar",
                    subtitle = state.error ?: "",
                )
            }
            state.alerts.isEmpty() -> item {
                EmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Sin alertas",
                    subtitle = "No hay alertas de servicio en este momento.",
                )
            }
            else -> items(state.alerts) { alert ->
                AlertCard(alert, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        item { Spacer(Modifier.width(24.dp)) }
    }
}

@Composable
private fun AlertCard(alert: ServiceAlert, modifier: Modifier = Modifier) {
    val color = LineColors.colorFor(alert.line)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(color),
            )
            Column(Modifier.padding(14.dp)) {
                Text(
                    alert.line.ifBlank { "Servicio" },
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.Bold,
                )
                if (alert.title.isNotBlank() && !alert.title.equals(alert.line, ignoreCase = true)) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        alert.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    alert.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
