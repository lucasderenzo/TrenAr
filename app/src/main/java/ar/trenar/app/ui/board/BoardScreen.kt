package ar.trenar.app.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.trenar.app.data.model.Arrival
import ar.trenar.app.ui.common.CountdownRing
import ar.trenar.app.ui.common.DelayBadge
import ar.trenar.app.ui.common.EmptyState
import ar.trenar.app.ui.common.Format
import ar.trenar.app.ui.common.FreshnessPill
import ar.trenar.app.ui.common.LineChip
import ar.trenar.app.ui.theme.LineColors
import ar.trenar.app.util.Geo
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    stationId: Int,
    onBack: () -> Unit,
    viewModel: BoardViewModel = viewModel(factory = BoardViewModel.factory(stationId)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var nowSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }

    // 1-second local tick for smooth countdowns
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowSec = System.currentTimeMillis() / 1000
        }
    }
    // periodic resync
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            viewModel.load(initial = false)
        }
    }

    val stationName = state.board?.station?.name ?: "Estación"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stationName, maxLines = 1, style = MaterialTheme.typography.titleLarge)
                        state.board?.let {
                            FreshnessPill(secondsAgo = (nowSec - it.loadedAtEpochSec).coerceAtLeast(0))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.pinToWidget() }) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "Fijar en widget",
                            tint = if (state.isPinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favorita",
                            tint = if (state.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { viewModel.load(initial = false) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null && state.board == null -> EmptyState(
                    icon = Icons.Outlined.WifiOff,
                    title = "No se pudo cargar",
                    subtitle = state.error ?: "Error",
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> BoardContent(
                    state = state,
                    nowSec = nowSec,
                    onFilter = viewModel::setFilter,
                )
            }
        }
    }
}

@Composable
private fun BoardContent(
    state: BoardUiState,
    nowSec: Long,
    onFilter: (String?) -> Unit,
) {
    val board = state.board ?: return
    val station = board.station
    val arrivals = state.filteredArrivals

    val radarTrains = remember(board.arrivals) {
        if (station == null) emptyList()
        else board.arrivals.mapNotNull { a ->
            val lat = a.trainLat
            val lng = a.trainLng
            if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) null
            else RadarTrain(
                bearingDeg = Geo.bearing(station.lat, station.lng, lat, lng),
                distanceMeters = Geo.haversine(station.lat, station.lng, lat, lng),
                color = LineColors.colorFor(a.line),
            )
        }
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Trenes en vivo",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                    ProximityRadar(trains = radarTrains)
                }
            }
        }

        if (state.destinations.size > 1) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.filterDestination == null,
                        onClick = { onFilter(null) },
                        label = { Text("Todos") },
                    )
                    state.destinations.take(4).forEach { dest ->
                        FilterChip(
                            selected = state.filterDestination == dest,
                            onClick = { onFilter(dest) },
                            label = { Text(dest, maxLines = 1) },
                        )
                    }
                }
            }
        }

        if (arrivals.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Train,
                    title = "Sin próximos trenes",
                    subtitle = "Puede que no haya servicio en este horario. Probá más tarde.",
                )
            }
        } else {
            items(arrivals, key = { it.serviceId + it.destination }) { arrival ->
                ArrivalCard(arrival = arrival, nowSec = nowSec)
            }
        }

        item { Spacer(Modifier.size(24.dp)) }
    }
}

@Composable
private fun ArrivalCard(arrival: Arrival, nowSec: Long) {
    val status = arrival.status()
    val etaSec = arrival.etaSecondsAt(nowSec)
    val lineColor = LineColors.colorFor(arrival.line)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountdownRing(seconds = if (arrival.cancelled) null else etaSec, status = status)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = arrival.destination,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (arrival.cancelled) TextDecoration.LineThrough else null,
                    maxLines = 1,
                )
                Spacer(Modifier.size(3.dp))
                LineChip(line = arrival.line, color = lineColor)
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    arrival.platform?.let {
                        Text(
                            "Andén $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Format.localTime(arrival.scheduledUtc)?.let { t ->
                        if (arrival.platform != null) {
                            Text("  ·  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "sale $t",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                arrival.note?.let {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            DelayBadge(status = status, delayMinutes = arrival.delayMinutes())
        }
    }
}
