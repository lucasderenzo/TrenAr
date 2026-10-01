package ar.trenar.app.data

import ar.trenar.app.data.local.StationCatalog
import ar.trenar.app.data.model.Arrival
import ar.trenar.app.data.model.BoardState
import ar.trenar.app.data.model.StationRef
import ar.trenar.app.data.remote.SofseApi
import ar.trenar.app.data.remote.dto.ArrivalResult
import ar.trenar.app.data.remote.dto.StationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import java.time.Instant

/** Single entry point for stations and live arrivals. */
class TrenRepository(
    private val api: SofseApi,
    val catalog: StationCatalog,
) {

    suspend fun searchStationsOnline(query: String): List<StationRef> = withContext(Dispatchers.IO) {
        api.searchStations(query).mapNotNull { it.toStationRef() }
    }

    /** Offline-first search backed by the bundled catalog. */
    suspend fun searchStations(query: String): List<StationRef> = catalog.search(query)

    suspend fun nearestStations(lat: Double, lng: Double, limit: Int = 8): List<StationRef> =
        catalog.nearest(lat, lng, limit)

    suspend fun station(id: Int): StationRef? = catalog.byId(id)

    suspend fun board(stationId: Int, cantidad: Int = 20): BoardState = withContext(Dispatchers.IO) {
        val station = catalog.byId(stationId)
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
