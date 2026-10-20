package ar.trenar.app.data.local

import android.content.Context
import ar.trenar.app.data.model.ScheduledDeparture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Serializable
private data class DayStations(
    // One list of HH:MM per station index (0 = terminalA), when a train passes that station.
    val toB: List<List<String>> = emptyList(),   // trains toward terminalB
    val toA: List<List<String>> = emptyList(),   // trains toward terminalA
)

@Serializable
private data class LinePlan(
    val terminalA: String = "",
    val terminalB: String = "",
    val source: String = "",
    val weekday: DayStations = DayStations(),
    val saturday: DayStations? = null,
    val sunday: DayStations? = null,
)

/**
 * Bundled static timetables for the no-realtime lines (Urquiza / Belgrano Norte).
 * Stores, per line / day-type / direction, the scheduled HH:MM at each station, and
 * computes the next upcoming passages for a given station in the local (AR) timezone.
 */
class ScheduleCatalog(private val context: Context) {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: Map<String, LinePlan>? = null
    private val mutex = Mutex()

    private suspend fun plans(): Map<String, LinePlan> {
        cache?.let { return it }
        return mutex.withLock { cache ?: load().also { cache = it } }
    }

    private suspend fun load(): Map<String, LinePlan> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.assets.open("schedules.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
                .removePrefix("﻿")
            json.decodeFromString<Map<String, LinePlan>>(text)
        }.getOrDefault(emptyMap())
    }

    suspend fun has(line: String): Boolean = plans().containsKey(line)

    /**
     * Next scheduled departures at the station at [stationIndex] along the line (0 = terminalA),
     * in both directions, labeled by destination.
     */
    suspend fun nextDepartures(
        line: String,
        stationIndex: Int,
        nowEpochSec: Long,
        limit: Int = 8,
    ): List<ScheduledDeparture> {
        val plan = plans()[line] ?: return emptyList()
        val out = ArrayList<ScheduledDeparture>(96)
        val today = Instant.ofEpochSecond(nowEpochSec).atZone(zone).toLocalDate()
        for (offset in 0..1) {
            val date = today.plusDays(offset.toLong())
            val day = dayStations(plan, date)
            day.toB.getOrNull(stationIndex)?.forEach { hhmm ->
                epochAt(date, hhmm)?.let { out.add(ScheduledDeparture(plan.terminalB, it)) }
            }
            day.toA.getOrNull(stationIndex)?.forEach { hhmm ->
                epochAt(date, hhmm)?.let { out.add(ScheduledDeparture(plan.terminalA, it)) }
            }
        }
        return out
            .filter { it.departureEpochSec >= nowEpochSec - 90 }
            .distinctBy { it.destination + it.departureEpochSec }
            .sortedBy { it.departureEpochSec }
            .take(limit)
    }

    /**
     * Estimated live positions of trains on a no-realtime line, derived from the bundled
     * timetable. Trips are reconstructed greedily (in travel order) from the per-station
     * passing times, then each in-progress trip is placed between the two stations it is
     * currently between, interpolating by elapsed fraction.
     *
     * @param coords ordered station coordinates (index 0 = terminalA), same order the schedule uses.
     */
    suspend fun estimatedTrains(
        line: String,
        coords: List<Pair<Double, Double>>,
        nowEpochSec: Long,
    ): List<ar.trenar.app.data.model.EstimatedTrain> {
        val plan = plans()[line] ?: return emptyList()
        val n = coords.size
        if (n < 2) return emptyList()
        val date = Instant.ofEpochSecond(nowEpochSec).atZone(zone).toLocalDate()
        val day = dayStations(plan, date)
        val out = ArrayList<ar.trenar.app.data.model.EstimatedTrain>()
        // toB travels 0 -> n-1 (towards terminalB); toA travels n-1 -> 0 (towards terminalA).
        placeDirection(day.toB, coords, (0 until n).toList(), date, nowEpochSec, plan.terminalB, out)
        placeDirection(day.toA, coords, (n - 1 downTo 0).toList(), date, nowEpochSec, plan.terminalA, out)
        return out
    }

    private fun placeDirection(
        times: List<List<String>>,
        coords: List<Pair<Double, Double>>,
        order: List<Int>,
        date: LocalDate,
        nowEpochSec: Long,
        destination: String,
        out: MutableList<ar.trenar.app.data.model.EstimatedTrain>,
    ) {
        if (times.isEmpty()) return
        val n = coords.size
        // consumable epoch lists per station index, sorted ascending
        val remaining = Array(n) { idx ->
            (times.getOrNull(idx) ?: emptyList())
                .mapNotNull { epochAt(date, it) }
                .sorted()
                .toMutableList()
        }
        val maxGapSec = 30 * 60L  // generous cap between adjacent stations (dwell + travel)

        // Greedily build trips in travel order; a station's leftover times start new trips there.
        for (j in order.indices) {
            val s = order[j]
            while (remaining[s].isNotEmpty()) {
                var curTime = remaining[s].removeAt(0)
                var curIdx = s
                // extend this trip forward through the remaining stations in travel order
                for (jj in (j + 1) until order.size) {
                    val ns = order[jj]
                    val cand = remaining[ns].firstOrNull { it > curTime && it - curTime <= maxGapSec }
                        ?: break
                    remaining[ns].remove(cand)
                    // is "now" within this segment? then place the train here
                    if (nowEpochSec in curTime..cand) {
                        val frac = if (cand > curTime)
                            (nowEpochSec - curTime).toDouble() / (cand - curTime) else 0.0
                        val (aLat, aLng) = coords[curIdx]
                        val (bLat, bLng) = coords[ns]
                        out.add(
                            ar.trenar.app.data.model.EstimatedTrain(
                                lat = aLat + (bLat - aLat) * frac,
                                lng = aLng + (bLng - aLng) * frac,
                                destination = destination,
                                fromIndex = curIdx,
                                toIndex = ns,
                                headingEast = bLng >= aLng,
                            ),
                        )
                    }
                    curTime = cand
                    curIdx = ns
                }
            }
        }
    }

    private fun dayStations(plan: LinePlan, date: LocalDate): DayStations = when {
        // Feriados nacionales run the Sunday/holiday timetable.
        ar.trenar.app.data.ArHolidays.isHoliday(date) -> plan.sunday ?: plan.weekday
        date.dayOfWeek == DayOfWeek.SATURDAY -> plan.saturday ?: plan.weekday
        date.dayOfWeek == DayOfWeek.SUNDAY -> plan.sunday ?: plan.weekday
        else -> plan.weekday
    }

    private fun epochAt(date: LocalDate, hhmm: String): Long? {
        val parts = hhmm.trim().split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return ZonedDateTime.of(date, LocalTime.of(h, m), zone).toEpochSecond()
    }
}
