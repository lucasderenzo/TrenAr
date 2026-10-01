package ar.trenar.app.ui.theme

import androidx.compose.ui.graphics.Color
import java.text.Normalizer

/** Maps a SOFSE line ("gerencia") name to a stable, distinguishable color. */
object LineColors {

    private data class Entry(val key: String, val color: Color)

    private val table = listOf(
        Entry("mitre", Color(0xFF1E88E5)),
        Entry("sarmiento", Color(0xFF00897B)),
        Entry("roca", Color(0xFF43A047)),
        Entry("san martin", Color(0xFFFB8C00)),
        Entry("belgrano sur", Color(0xFFF9A825)),
        Entry("belgrano norte", Color(0xFF8E24AA)),
        Entry("urquiza", Color(0xFF00ACC1)),
        Entry("tren de la costa", Color(0xFF26A69A)),
        Entry("costa", Color(0xFF26A69A)),
    )

    private val fallback = listOf(
        Color(0xFF1E88E5), Color(0xFF00897B), Color(0xFF43A047), Color(0xFFFB8C00),
        Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF5E35B1),
    )

    fun colorFor(line: String?): Color {
        if (line.isNullOrBlank()) return Color(0xFF607D8B)
        val n = normalize(line)
        table.firstOrNull { n.contains(it.key) }?.let { return it.color }
        val idx = (n.hashCode() and Int.MAX_VALUE) % fallback.size
        return fallback[idx]
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
