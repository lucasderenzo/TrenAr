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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.trenar.app.R
import ar.trenar.app.ui.common.BikeStationRow
import ar.trenar.app.ui.common.EmptyState
import ar.trenar.app.ui.common.StationArrivalRow
import ar.trenar.app.util.MapsIntent
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
    val ctx = LocalContext.current
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
        item { CercanasHeader(onOpenSearch, state.currentAddress) }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.mode == CercanasMode.TREN,
                    onClick = { viewModel.setMode(CercanasMode.TREN) },
                    label = { Text(stringResource(R.string.tren)) },
                    leadingIcon = { Icon(Icons.Filled.Train, contentDescription = null, modifier = Modifier.width(20.dp)) },
                )
                FilterChip(
                    selected = state.mode == CercanasMode.ECOBICI,
                    onClick = { viewModel.setMode(CercanasMode.ECOBICI) },
                    label = { Text(stringResource(R.string.ecobici)) },
                    leadingIcon = { Icon(Icons.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.width(20.dp)) },
                )
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.radio),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                listOf(
                    1000 to R.string.radio_1km,
                    3000 to R.string.radio_3km,
                    Int.MAX_VALUE to R.string.radio_sin,
                ).forEach { (m, labelRes) ->
                    FilterChip(
                        selected = state.radiusMeters == m,
                        onClick = { viewModel.setRadius(m) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
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
                    title = stringResource(R.string.no_pudimos_ubicarte),
                    subtitle = stringResource(R.string.activa_gps),
                )
            }
            empty -> item {
                EmptyState(
                    icon = if (tren) Icons.Outlined.NearMe else Icons.Filled.DirectionsBike,
                    title = stringResource(if (tren) R.string.cercanas_sin_estaciones_title else R.string.cercanas_sin_ecobici_title),
                    subtitle = stringResource(if (tren) R.string.cercanas_sin_estaciones_sub else R.string.cercanas_sin_ecobici_sub),
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
                BikeStationRow(
                    item = item,
                    isFavorite = state.bikeFavorites.contains(item.station.id),
                    onToggleFavorite = { viewModel.toggleBikeFavorite(item.station.id) },
                    onDirections = { MapsIntent.directionsTo(ctx, item.station.lat, item.station.lng, item.station.name) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item { Spacer(Modifier.width(24.dp)) }
    }
}

@Composable
private fun CercanasHeader(onOpenSearch: () -> Unit, address: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.nav_cercanas),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.buscar_estacion))
            }
        }
        if (address != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 1.dp, bottom = 2.dp),
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                stringResource(R.string.encontra_cerca_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.encontra_cerca_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onGrant) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.usar_ubicacion))
            }
        }
    }
}
