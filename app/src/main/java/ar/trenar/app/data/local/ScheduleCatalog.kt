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

    private fun dayStations(plan: LinePlan, date: LocalDate): DayStations = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> plan.saturday ?: plan.weekday
        DayOfWeek.SUNDAY -> plan.sunday ?: plan.weekday
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
