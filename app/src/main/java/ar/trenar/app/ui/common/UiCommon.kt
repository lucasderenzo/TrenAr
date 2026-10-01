package ar.trenar.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ar.trenar.app.data.model.DelayStatus
import ar.trenar.app.ui.theme.CancelledRed
import ar.trenar.app.ui.theme.MajorRed
import ar.trenar.app.ui.theme.MinorAmber
import ar.trenar.app.ui.theme.OnTimeGreen

fun statusColor(status: DelayStatus): Color = when (status) {
    DelayStatus.ON_TIME -> OnTimeGreen
    DelayStatus.MINOR -> MinorAmber
    DelayStatus.MAJOR -> MajorRed
    DelayStatus.CANCELLED -> CancelledRed
    DelayStatus.UNKNOWN -> Color(0xFF90A4AE)
}

@Composable
fun DelayBadge(status: DelayStatus, delayMinutes: Int?) {
    val (label, color) = when (status) {
        DelayStatus.ON_TIME -> "En hora" to OnTimeGreen
        DelayStatus.MINOR -> ("+${delayMinutes ?: 1} min") to MinorAmber
        DelayStatus.MAJOR -> ("+${delayMinutes ?: 10} min") to MajorRed
        DelayStatus.CANCELLED -> "Cancelado" to CancelledRed
        DelayStatus.UNKNOWN -> "s/d" to Color(0xFF90A4AE)
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun LineChip(line: String, color: Color) {
    if (line.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = line,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun FreshnessPill(secondsAgo: Long) {
    val stale = secondsAgo > 120
    val color = if (stale) MinorAmber else OnTimeGreen
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "actualizado ${Format.freshness(secondsAgo)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(46.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

val ScreenPadding = PaddingValues(horizontal = 16.dp)

@Composable
fun VSpace(height: Int) {
    Spacer(Modifier.height(height.dp))
}
