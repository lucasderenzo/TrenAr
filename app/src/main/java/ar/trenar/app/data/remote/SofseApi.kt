package ar.trenar.app.data.remote

import ar.trenar.app.data.remote.dto.ArrivalsResponse
import ar.trenar.app.data.remote.dto.GerenciaDto
import ar.trenar.app.data.remote.dto.StationDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SofseApi {

    @GET("infraestructura/estaciones")
    suspend fun searchStations(
        @Query("nombre") nombre: String,
    ): List<StationDto>

    @GET("infraestructura/gerencias")
    suspend fun gerencias(
        @Query("idEmpresa") idEmpresa: Int = 1,
    ): List<GerenciaDto>

    @GET("arribos/estacion/{id}")
    suspend fun arrivals(
        @Path("id") stationId: Int,
        @Query("cantidad") cantidad: Int? = null,
        @Query("hasta") hasta: Int? = null,
        @Query("ramal") ramal: Int? = null,
    ): ArrivalsResponse
}
