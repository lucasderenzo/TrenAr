package ar.trenar.app.ui.theme

import androidx.compose.ui.graphics.Color
import java.text.Normalizer

/** Maps a SOFSE line ("gerencia") name to a stable color and a 2-letter code. */
object LineColors {

    private data class Entry(val key: String, val color: Color)

    private val table = listOf(
        Entry("sarmiento", Color(0xFFD6007E)),      // magenta
        Entry("mitre", Color(0xFF0EA5E0)),          // celeste
        Entry("roca", Color(0xFF2E9E5B)),           // verde
        Entry("san martin", Color(0xFFF57C00)),     // naranja
        Entry("belgrano sur", Color(0xFFF9A825)),   // amarillo
        Entry("belgrano norte", Color(0xFF8E24AA)),
        Entry("urquiza", Color(0xFF00ACC1)),
        Entry("tren de la costa", Color(0xFF26A69A)),
        Entry("costa", Color(0xFF26A69A)),
        Entry("regionales", Color(0xFF7E57C2)),
    )

    private val fallback = listOf(
        Color(0xFF0EA5E0), Color(0xFF2E9E5B), Color(0xFFF57C00), Color(0xFFD6007E),
        Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFF7E57C2), Color(0xFF26A69A),
    )

    fun colorFor(line: String?): Color {
        if (line.isNullOrBlank()) return Color(0xFF607D8B)
        val n = normalize(line)
        table.firstOrNull { n.contains(it.key) }?.let { return it.color }
        val idx = (n.hashCode() and Int.MAX_VALUE) % fallback.size
        return fallback[idx]
    }

    /** Two-letter code for a line: Sarmiento→SA, Mitre→MI, Roca→RO, San Martín→SM, Belgrano Sur→BS. */
    fun code(line: String?): String {
        if (line.isNullOrBlank()) return "--"
        val cleaned = line.substringBefore('(').trim()
        val words = cleaned.split(' ', '.', '-', '/')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.first().isLetter() }
        return when {
            words.isEmpty() -> line.take(2).uppercase()
            words.size == 1 -> words[0].take(2).uppercase()
            else -> (words[0].first().toString() + words[1].first().toString()).uppercase()
        }
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
