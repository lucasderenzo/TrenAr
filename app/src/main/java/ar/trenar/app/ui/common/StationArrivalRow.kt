package ar.trenar.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ar.trenar.app.data.model.StationWithNext
import ar.trenar.app.ui.theme.OnTimeGreen
import ar.trenar.app.util.Geo

/**
 * A station row: line-code badge + name (+ line/distance), and on the right the next train's
 * ETA in minutes with its destination below ("a Moreno").
 */
@Composable
fun StationArrivalRow(
    item: StationWithNext,
    nowSec: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val st = item.station
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationMonogram(stationName = st.name, line = st.line)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    st.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                val subtitle = buildString {
                    if (st.line.isNotBlank()) append(st.line)
                    st.distanceMeters?.let {
                        if (isNotEmpty()) append(" · ")
                        append("a ${Geo.formatDistance(it)}")
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            NextArrivalCell(item, nowSec)
        }
    }
}

@Composable
private fun NextArrivalCell(item: StationWithNext, nowSec: Long) {
    val next = item.next
    when {
        item.loading -> Text(
            "…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        next == null -> Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> {
            val eta = next.etaSecondsAt(nowSec)
            val live = next.trainLat != null && next.trainLng != null
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                if (eta != null && eta < 60) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (live) LiveDot()
                        Text(
                            "llegando",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnTimeGreen,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (live) {
                            LiveDot()
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            eta?.let { "${it / 60}" } ?: "–",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor(next.status()),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                Text(
                    "a ${next.destination}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun LiveDot() {
    Spacer(
        Modifier
            .padding(end = 5.dp)
            .size(7.dp)
            .background(OnTimeGreen, CircleShape),
    )
}
