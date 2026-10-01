package ar.trenar.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.trenar.app.data.model.DelayStatus

/** Circular countdown that fills as the train gets closer (cap = 15 min). */
@Composable
fun CountdownRing(
    seconds: Int?,
    status: DelayStatus,
    modifier: Modifier = Modifier,
    diameter: Dp = 72.dp,
) {
    val capSeconds = 15f * 60f
    val target = seconds?.let {
        (1f - (it.coerceIn(0, capSeconds.toInt()) / capSeconds)).coerceIn(0f, 1f)
    } ?: 0f
    val progress by animateFloatAsState(target, tween(600), label = "ringProgress")
    val ringColor by animateColorAsState(statusColor(status), tween(400), label = "ringColor")
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val (big, unit) = Format.etaBig(seconds)

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.11f
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - stroke, size.height - stroke,
            )
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = big,
                fontWeight = FontWeight.Bold,
                fontSize = if (big.length > 3) 18.sp else 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
