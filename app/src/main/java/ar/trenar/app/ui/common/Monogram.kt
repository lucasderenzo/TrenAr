package ar.trenar.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.trenar.app.ui.theme.LineColors

/** A colored tile with the line's 2-letter code (SA / MI / RO / SM …). */
@Composable
fun LineBadge(
    line: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    corner: Dp = 14.dp,
) {
    val bg = LineColors.colorFor(line)
    val fg = if (bg.luminance() > 0.6f) Color(0xFF14202E) else Color.White
    Box(
        modifier = modifier
            .size(size)
            .background(bg, RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = LineColors.code(line),
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.34f).sp,
        )
    }
}

/** Station tile: shows the line code colored by line (foto 1 style). */
@Composable
fun StationMonogram(
    stationName: String,
    line: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    LineBadge(line = line, modifier = modifier, size = size)
}
