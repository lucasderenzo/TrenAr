package ar.trenar.app.data

import ar.trenar.app.data.local.ScheduleCatalog
import ar.trenar.app.data.local.StationCatalog
import ar.trenar.app.data.model.Arrival
import ar.trenar.app.data.model.BoardState
import ar.trenar.app.data.model.ServiceAlert
import ar.trenar.app.data.model.StationRef
import ar.trenar.app.data.model.StationWithNext
import ar.trenar.app.data.remote.SofseApi
import ar.trenar.app.data.remote.dto.ArrivalResult
import ar.trenar.app.data.remote.dto.StationDto
import ar.trenar.app.util.Geo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import java.time.Instant

/** Single entry point for stations and live arrivals. */
class TrenRepository(
    private val api: SofseApi,
    val catalog: StationCatalog,
    private val schedules: ScheduleCatalog,
) {

    private val ecobici = ar.trenar.app.data.remote.EcobiciClient()

    /** Live Ecobici dock stations (public GBFS feed). */
    suspend fun ecobiciStations(): List<ar.trenar.app.data.model.BikeStation> = ecobici.stations()

    /** Nearest Ecobici stations to a point, with distance + walking time. */
    suspend fun nearestBikes(
        lat: Double,
        lng: Double,
        limit: Int = 10,
    ): List<ar.trenar.app.data.model.BikeNearby> {
        return ecobici.stations()
            .map { it to Geo.haversine(lat, lng, it.lat, it.lng) }
            .sortedBy { it.second }
            .take(limit)
            .map { (st, dist) ->
                ar.trenar.app.data.model.BikeNearby(
                    station = st,
                    distanceMeters = dist,
                    walkSeconds = (dist / 1.35).toInt(),
                )
            }
    }

    suspend fun searchStationsOnline(query: String): List<StationRef> = withContext(Dispatchers.IO) {
        api.searchStations(query).mapNotNull { it.toStationRef() }
    }

    /** Offline-first search backed by the bundled catalog. */
    suspend fun searchStations(query: String): List<StationRef> = catalog.search(query)

    suspend fun nearestStations(lat: Double, lng: Double, limit: Int = 8): List<StationRef> =
        catalog.nearest(lat, lng, limit)

    suspend fun station(id: Int): StationRef? = catalog.byId(id)

    /** Soonest upcoming train at a station, or null if none / no service. */
    suspend fun nextArrival(stationId: Int): Arrival? = withContext(Dispatchers.IO) {
        // Lines without a live feed (Urquiza / Belgrano Norte) have no arrivals to fetch.
        val st = catalog.byId(stationId)
        if (st != null && !LineInfo.hasRealtime(st.line)) return@withContext null
        val resp = runCatching { api.arrivals(stationId, cantidad = 4) }.getOrNull() ?: return@withContext null
        val serverTs = if (resp.timestamp > 0) resp.timestamp else System.currentTimeMillis() / 1000
        resp.results.mapNotNull { it.toArrival(serverTs) }
            .filter { !it.cancelled }
            .minByOrNull { it.etaSeconds ?: Int.MAX_VALUE }
    }

    /** All catalog stations belonging to a line. */
    suspend fun stationsOfLine(line: String): List<StationRef> =
        catalog.all().filter { it.line.equals(line, ignoreCase = true) }

    /** The list of lines that actually have stations, in display order. */
    suspend fun availableLines(): List<String> {
        val present = catalog.all().map { it.line }.filter { it.isNotBlank() }.toSet()
        val order = listOf(
            "Sarmiento", "Mitre", "Roca", "San Martín", "Belgrano Sur",
            "Tren de la Costa", "Urquiza", "Belgrano Norte", "Regionales",
        )
        return order.filter { it in present } + present.filter { it !in order }
    }

    /**
     * Live trains currently reported on a line, de-duplicated. Samples several stations of the
     * line and collects the services that carry a GPS position.
     */
    suspend fun liveTrains(line: String, sampleStations: Int = 12): List<Arrival> = coroutineScope {
        if (!LineInfo.hasRealtime(line)) return@coroutineScope emptyList()
        val stations = stationsOfLine(line)
        if (stations.isEmpty()) return@coroutineScope emptyList()
        val step = (stations.size / sampleStations).coerceAtLeast(1)
        val sample = stations.filterIndexed { i, _ -> i % step == 0 }.take(sampleStations)
        val responses = sample.map { st ->
            async { runCatching { api.arrivals(st.id, cantidad = 12) }.getOrNull() }
        }.awaitAll()
        responses.filterNotNull()
            .flatMap { resp ->
                val ts = if (resp.timestamp > 0) resp.timestamp else System.currentTimeMillis() / 1000
                resp.results.mapNotNull { it.toArrival(ts) }
            }
            .filter { it.trainLat != null && it.trainLng != null && !(it.trainLat == 0.0 && it.trainLng == 0.0) }
            .distinctBy { it.serviceId }
    }

    /** Enrich a list of stations with each one's next train, fetched in parallel. */
    suspend fun withNextArrivals(stations: List<StationRef>): List<StationWithNext> = coroutineScope {
        stations.map { st -> async { StationWithNext(st, nextArrival(st.id)) } }.awaitAll()
    }

    /** Current service alerts across all lines. */
    suspend fun alerts(): List<ServiceAlert> = withContext(Dispatchers.IO) {
        val gerencias = runCatching { api.gerencias(1) }.getOrNull() ?: return@withContext emptyList()
        gerencias.flatMap { g ->
            val line = g.nombre ?: ""
            g.alerta.mapNotNull { a ->
                val body = a.contenido?.trim().orEmpty()
                if (body.isBlank()) null
                else ServiceAlert(
                    line = line,
                    title = a.titulo?.trim()?.takeIf { it.isNotBlank() } ?: (g.estado?.mensaje?.trim()?.takeIf { it.isNotBlank() } ?: line),
                    body = body,
                    ramalId = a.ramalId,
                )
            }
        }
    }

    suspend fun board(stationId: Int, cantidad: Int = 20): BoardState = withContext(Dispatchers.IO) {
        val station = catalog.byId(stationId)
        // Urquiza / Belgrano Norte: no live feed — serve the bundled schedule + official-timetable notice.
        if (station != null && !LineInfo.hasRealtime(station.line)) {
            val now = System.currentTimeMillis() / 1000
            val lineStations = stationsOfLine(station.line).sortedBy { it.id }
            val index = lineStations.indexOfFirst { it.id == stationId }
            val scheduled = if (index >= 0) {
                runCatching { schedules.nextDepartures(station.line, index, now, limit = 10) }
                    .getOrDefault(emptyList())
            } else {
                emptyList()
            }
            return@withContext BoardState(
                station = station,
                arrivals = emptyList(),
                serverTimestamp = now,
                loadedAtEpochSec = now,
                realtime = false,
                scheduleUrl = LineInfo.scheduleUrl(station.line),
                operator = LineInfo.operator(station.line),
                scheduled = scheduled,
            )
        }
        val resp = api.arrivals(stationId, cantidad = cantidad)
        val serverTs = if (resp.timestamp > 0) resp.timestamp else System.currentTimeMillis() / 1000
        val arrivals = resp.results
            .mapNotNull { it.toArrival(serverTs) }
            .sortedBy { it.etaSeconds ?: Int.MAX_VALUE }
        BoardState(
            station = station,
            arrivals = arrivals,
            serverTimestamp = serverTs,
            loadedAtEpochSec = System.currentTimeMillis() / 1000,
        )
    }

    private fun StationDto.toStationRef(): StationRef? {
        val id = idEstacion.toIntOrNull() ?: return null
        val lat = latitud?.toDoubleOrNull() ?: 0.0
        val lng = longitud?.toDoubleOrNull() ?: 0.0
        return StationRef(id, nombre, lat, lng, ramales)
    }

    private fun ArrivalResult.toArrival(serverTs: Long): Arrival? {
        val s = servicio ?: return null
        val a = arribo
        val eta = a?.segundos
        val scheduled = a?.salida?.programada
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val cancelled = s.cancelacion?.let { it != JsonNull } ?: false
        val delay = if (scheduled != null && eta != null) {
            (serverTs + eta) - scheduled.epochSecond
        } else {
            null
        }
        return Arrival(
            serviceId = s.id ?: s.numero?.toString() ?: (a?.orden?.toString() ?: "?"),
            destination = s.ramal?.cabeceraFinal?.nombre
                ?: s.ramal?.nombre?.substringAfterLast('-')?.trim()
                ?: "Destino s/d",
            ramalName = s.ramal?.nombre ?: "",
            line = s.gerencia?.nombre ?: "",
            platform = a?.anden?.nombre,
            etaSeconds = eta,
            fetchedAtEpochSec = System.currentTimeMillis() / 1000,
            scheduledUtc = scheduled,
            delaySeconds = delay,
            toleranceSeconds = s.ramal?.tolerancia,
            cancelled = cancelled,
            direction = s.sentido,
            trainLat = s.location?.lat,
            trainLng = s.location?.long,
            note = s.leyenda?.takeIf { it.isNotBlank() },
        )
    }
}
