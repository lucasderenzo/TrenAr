package ar.trenar.app.ui.favoritos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.trenar.app.ui.common.BikeStationRow
import ar.trenar.app.ui.common.EmptyState
import ar.trenar.app.ui.common.StationArrivalRow
import ar.trenar.app.ui.common.TabHeader
import ar.trenar.app.util.MapsIntent
import kotlinx.coroutines.delay

@Composable
fun FavoritosScreen(
    onOpenStation: (Int) -> Unit,
    viewModel: FavoritosViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var nowSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000); nowSec = System.currentTimeMillis() / 1000
        }
    }

    val hasTrains = state.items.isNotEmpty()
    val hasBikes = state.bikeItems.isNotEmpty()
    val showSectionLabels = hasTrains && hasBikes

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { TabHeader("Favoritos") }

        if (!hasTrains && !hasBikes) {
            item {
                EmptyState(
                    icon = Icons.Outlined.StarOutline,
                    title = "Todavía no marcaste favoritas",
                    subtitle = "Tocá la estrella en una estación o en una estación de Ecobici para tenerla a mano.",
                )
            }
        }

        if (hasTrains) {
            if (showSectionLabels) item { SectionLabel("Trenes") }
            items(state.items, key = { "tren_${it.station.id}" }) { item ->
                StationArrivalRow(
                    item = item,
                    nowSec = nowSec,
                    onClick = { onOpenStation(item.station.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (hasBikes) {
            if (showSectionLabels) item { SectionLabel("Ecobici") }
            items(state.bikeItems, key = { "bike_${it.station.id}" }) { item ->
                BikeStationRow(
                    item = item,
                    isFavorite = true,
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
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 4.dp),
    )
}
