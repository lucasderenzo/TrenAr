package ar.trenar.app.data

import java.text.Normalizer

/**
 * Lines run by private concessionaires that have NO public real-time feed.
 * For these the app shows the stations on the map/catalog but, instead of live
 * arrivals, surfaces a clear warning linking to the operator's official timetable.
 */
object LineInfo {

    private data class NoRealtime(
        val key: String,          // normalized match on the line name
        val operator: String,     // who runs it
        val scheduleUrl: String,  // official timetable
    )

    private val noRealtime = listOf(
        NoRealtime(
            key = "urquiza",
            operator = "Metrovías",
            scheduleUrl = "https://metrovias.com.ar/index.php/horarios-del-servicio-linea-urquiza/",
        ),
        NoRealtime(
            key = "belgrano norte",
            operator = "Ferrovías",
            scheduleUrl = "https://ferrovias.com.ar/horarios/",
        ),
    )

    private fun entry(line: String?): NoRealtime? {
        val n = normalize(line)
        if (n.isBlank()) return null
        return noRealtime.firstOrNull { n.contains(it.key) }
    }

    /** True when this line has live data (the SOFSE lines); false for Urquiza / Belgrano Norte. */
    fun hasRealtime(line: String?): Boolean = entry(line) == null

    /** Official timetable URL for a no-realtime line, or null. */
    fun scheduleUrl(line: String?): String? = entry(line)?.scheduleUrl

    /** Operator name for a no-realtime line, or null. */
    fun operator(line: String?): String? = entry(line)?.operator

    private fun normalize(s: String?): String =
        Normalizer.normalize((s ?: "").trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
