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
private data class DayPlan(
    val outbound: List<String> = emptyList(),
    val inbound: List<String> = emptyList(),
)

@Serializable
private data class LinePlan(
    val terminalA: String = "",
    val terminalB: String = "",
    val endToEndMinutes: Int = 0,
    val cumMinutes: List<Int> = emptyList(),
    val weekday: DayPlan = DayPlan(),
    val saturday: DayPlan? = null,
    val sunday: DayPlan? = null,
)

/**
 * Bundled static timetables for the no-realtime lines (Urquiza / Belgrano Norte).
 * Computes the next scheduled passages at a station for both directions.
 */
class ScheduleCatalog(private val context: Context) {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: Map<String, LinePlan>? = null
    private val mutex = Mutex()

    private suspend fun plans(): Map<String, LinePlan> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: load().also { cache = it }
        }
    }

    private suspend fun load(): Map<String, LinePlan> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.assets.open("schedules.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
                .removePrefix("﻿")
            json.decodeFromString<Map<String, LinePlan>>(text)
        }.getOrDefault(emptyMap())
    }

    /** True when there is a bundled timetable for [line]. */
    suspend fun has(line: String): Boolean = plans().containsKey(line)

    /**
     * Next scheduled departures at the station whose position along the line is [stationIndex]
     * (0 = terminalA). Returns passages in both directions, labeled by destination.
     */
    suspend fun nextDepartures(
        line: String,
        stationIndex: Int,
        nowEpochSec: Long,
        limit: Int = 8,
    ): List<ScheduledDeparture> {
        val plan = plans()[line] ?: return emptyList()
        if (stationIndex < 0 || stationIndex >= plan.cumMinutes.size) return emptyList()
        val cum = plan.cumMinutes[stationIndex]
        val toEnd = (plan.endToEndMinutes - cum).coerceAtLeast(0)

        val out = ArrayList<ScheduledDeparture>(64)
        val today = Instant.ofEpochSecond(nowEpochSec).atZone(zone).toLocalDate()
        for (offset in 0..1) {
            val date = today.plusDays(offset.toLong())
            val day = dayPlan(plan, date)
            day.outbound.forEach { hhmm ->
                epochAt(date, hhmm, cum)?.let { out.add(ScheduledDeparture(plan.terminalB, it)) }
            }
            day.inbound.forEach { hhmm ->
                epochAt(date, hhmm, toEnd)?.let { out.add(ScheduledDeparture(plan.terminalA, it)) }
            }
        }
        return out
            .filter { it.departureEpochSec >= nowEpochSec - 90 }
            .sortedBy { it.departureEpochSec }
            .take(limit)
    }

    private fun dayPlan(plan: LinePlan, date: LocalDate): DayPlan = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> plan.saturday ?: plan.weekday
        DayOfWeek.SUNDAY -> plan.sunday ?: plan.weekday
        else -> plan.weekday
    }

    private fun epochAt(date: LocalDate, hhmm: String, plusMinutes: Int): Long? {
        val parts = hhmm.trim().split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        // tolerate "24:10" style past-midnight entries by rolling into the next day
        val extraDays = h / 24
        val hour = h % 24
        if (hour !in 0..23 || m !in 0..59) return null
        val base = ZonedDateTime.of(date.plusDays(extraDays.toLong()), LocalTime.of(hour, m), zone)
        return base.toEpochSecond() + plusMinutes * 60L
    }
}
