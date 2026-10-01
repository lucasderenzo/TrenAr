package ar.trenar.app.ui.favoritos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.trenar.app.ui.common.EmptyState
import ar.trenar.app.ui.common.StationArrivalRow
import ar.trenar.app.ui.common.TabHeader
import kotlinx.coroutines.delay

@Composable
fun FavoritosScreen(
    onOpenStation: (Int) -> Unit,
    viewModel: FavoritosViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var nowSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000); nowSec = System.currentTimeMillis() / 1000
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { TabHeader("Favoritos") }

        if (state.items.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.StarOutline,
                    title = "Todavía no marcaste favoritas",
                    subtitle = "Tocá la estrella en cualquier estación para tenerla siempre a mano.",
                )
            }
        } else {
            items(state.items, key = { it.station.id }) { item ->
                StationArrivalRow(
                    item = item,
                    nowSec = nowSec,
                    onClick = { onOpenStation(item.station.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        item { Spacer(Modifier.width(24.dp)) }
    }
}
