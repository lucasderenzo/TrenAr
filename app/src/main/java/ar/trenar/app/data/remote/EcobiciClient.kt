package ar.trenar.app.data.remote

import ar.trenar.app.data.model.BikeStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Public Ecobici GBFS feed (no credentials). Merges station info + live status. */
class EcobiciClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun stations(): List<BikeStation> = withContext(Dispatchers.IO) {
        val info = fetch<InfoResp>("$BASE/station_information.json") ?: return@withContext emptyList()
        val status = fetch<StatusResp>("$BASE/station_status.json")
        val statusById = status?.data?.stations?.associateBy { it.stationId } ?: emptyMap()
        info.data.stations.mapNotNull { s ->
            if (s.lat == 0.0 && s.lon == 0.0) return@mapNotNull null
            val st = statusById[s.stationId]
            BikeStation(
                id = s.stationId,
                name = s.name,
                lat = s.lat,
                lng = s.lon,
                bikes = st?.numBikesAvailable ?: 0,
                docks = st?.numDocksAvailable ?: 0,
                renting = st?.isRenting ?: false,
            )
        }
    }

    private inline fun <reified T> fetch(url: String): T? {
        return try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return null
                if (!resp.isSuccessful) return null
                json.decodeFromString<T>(body)
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val BASE = "https://buenosaires.publicbikesystem.net/customer/gbfs/v2/en"
    }

    @Serializable
    private data class InfoResp(val data: InfoData = InfoData())
    @Serializable
    private data class InfoData(val stations: List<InfoStation> = emptyList())
    @Serializable
    private data class InfoStation(
        @SerialName("station_id") val stationId: String = "",
        val name: String = "",
        val lat: Double = 0.0,
        val lon: Double = 0.0,
    )

    @Serializable
    private data class StatusResp(val data: StatusData = StatusData())
    @Serializable
    private data class StatusData(val stations: List<StatusStation> = emptyList())
    @Serializable
    private data class StatusStation(
        @SerialName("station_id") val stationId: String = "",
        @SerialName("num_bikes_available") val numBikesAvailable: Int = 0,
        @SerialName("num_docks_available") val numDocksAvailable: Int = 0,
        @SerialName("is_renting") val isRenting: Boolean = false,
    )
}
