package ar.trenar.app.ui.cercanas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.trenar.app.ui.common.BikeStationRow
import ar.trenar.app.ui.common.EmptyState
import ar.trenar.app.ui.common.StationArrivalRow
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CercanasScreen(
    onOpenStation: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: CercanasViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    var nowSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }

    LaunchedEffect(permission.status.isGranted) {
        if (permission.status.isGranted) viewModel.refresh()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000); nowSec = System.currentTimeMillis() / 1000
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { CercanasHeader(onOpenSearch) }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.mode == CercanasMode.TREN,
                    onClick = { viewModel.setMode(CercanasMode.TREN) },
                    label = { Text("Tren") },
                    leadingIcon = { Icon(Icons.Filled.Train, contentDescription = null, modifier = Modifier.width(20.dp)) },
                )
                FilterChip(
                    selected = state.mode == CercanasMode.ECOBICI,
                    onClick = { viewModel.setMode(CercanasMode.ECOBICI) },
                    label = { Text("Ecobici") },
                    leadingIcon = { Icon(Icons.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.width(20.dp)) },
                )
            }
        }

        val tren = state.mode == CercanasMode.TREN
        val empty = if (tren) state.items.isEmpty() else state.bikeItems.isEmpty()

        when {
            !permission.status.isGranted -> item {
                LocationPrompt(
                    onGrant = { permission.launchPermissionRequest() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            state.loading && empty -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.locationUnavailable -> item {
                EmptyState(
                    icon = Icons.Filled.LocationOn,
                    title = "No pudimos ubicarte",
                    subtitle = "Activá el GPS y probá de nuevo, o buscá tu estación.",
                )
            }
            empty -> item {
                EmptyState(
                    icon = if (tren) Icons.Outlined.NearMe else Icons.Filled.DirectionsBike,
                    title = if (tren) "Sin estaciones cerca" else "Sin Ecobici cerca",
                    subtitle = if (tren) "Buscá tu estación por nombre." else "No encontramos estaciones de Ecobici cerca tuyo.",
                )
            }
            tren -> items(state.items, key = { it.station.id }) { item ->
                StationArrivalRow(
                    item = item,
                    nowSec = nowSec,
                    onClick = { onOpenStation(item.station.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            else -> items(state.bikeItems, key = { it.station.id }) { item ->
                BikeStationRow(item = item, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        item { Spacer(Modifier.width(24.dp)) }
    }
}

@Composable
private fun CercanasHeader(onOpenSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Cercanas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenSearch) {
            Icon(Icons.Filled.Search, contentDescription = "Buscar estación")
        }
    }
}

@Composable
private fun LocationPrompt(onGrant: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Encontrá lo que tenés cerca",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Con tu ubicación te mostramos las estaciones de tren y las de Ecobici más próximas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onGrant) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Usar mi ubicación")
            }
        }
    }
}
