package ar.trenar.app.ui.board

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A single train plotted on the radar. */
data class RadarTrain(
    val bearingDeg: Double,
    val distanceMeters: Double,
    val color: Color,
)

/**
 * A compact "radar" that plots live trains around the station by bearing + distance,
 * with a rotating sweep. No map tiles, no API key.
 */
@Composable
fun ProximityRadar(
    trains: List<RadarTrain>,
    modifier: Modifier = Modifier,
) {
    val maxDistance = (trains.maxOfOrNull { it.distanceMeters } ?: 3000.0)
        .coerceIn(1500.0, 15000.0)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val centerColor = MaterialTheme.colorScheme.primary
    val sweepColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )

    Box(modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(cx, cy) - 8f

            // concentric rings
            for (i in 1..3) {
                drawCircle(
                    color = gridColor,
                    radius = radius * i / 3f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f),
                )
            }
            // cross-hairs
            drawLine(gridColor, Offset(cx - radius, cy), Offset(cx + radius, cy), strokeWidth = 1f)
            drawLine(gridColor, Offset(cx, cy - radius), Offset(cx, cy + radius), strokeWidth = 1f)

            // rotating sweep wedge
            val rad = Math.toRadians(sweep.toDouble() - 90.0)
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.08f to sweepColor,
                    0.12f to Color.Transparent,
                    center = Offset(cx, cy),
                ),
                startAngle = sweep - 90f,
                sweepAngle = 44f,
                useCenter = true,
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            )
            drawLine(
                color = centerColor.copy(alpha = 0.5f),
                start = Offset(cx, cy),
                end = Offset(cx + radius * cos(rad).toFloat(), cy + radius * sin(rad).toFloat()),
                strokeWidth = 2f,
            )

            // trains
            for (t in trains) {
                val r = (t.distanceMeters / maxDistance).coerceIn(0.0, 1.0) * radius
                val a = Math.toRadians(t.bearingDeg - 90.0)
                val x = cx + (r * cos(a)).toFloat()
                val y = cy + (r * sin(a)).toFloat()
                drawCircle(t.color.copy(alpha = 0.25f), radius = 12f, center = Offset(x, y))
                drawCircle(t.color, radius = 6f, center = Offset(x, y))
            }

            // station at center
            drawCircle(centerColor, radius = 7f, center = Offset(cx, cy))
            drawCircle(Color.White, radius = 3f, center = Offset(cx, cy))
        }

        if (trains.isEmpty()) {
            Text(
                text = "Sin trenes con posición en vivo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
