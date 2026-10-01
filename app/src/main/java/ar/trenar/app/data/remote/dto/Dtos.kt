package ar.trenar.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class StationDto(
    val nombre: String = "",
    @SerialName("id_estacion") val idEstacion: String = "",
    val latitud: String? = null,
    val longitud: String? = null,
    @SerialName("incluida_en_ramales") val ramales: List<Int> = emptyList(),
)

@Serializable
data class ArrivalsResponse(
    val timestamp: Long = 0,
    val results: List<ArrivalResult> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ArrivalResult(
    val arribo: Arribo? = null,
    val servicio: Servicio? = null,
)

@Serializable
data class Arribo(
    val anden: NamedId? = null,
    val orden: Int? = null,
    val nombre: String? = null,
    val parada: Boolean? = null,
    val salida: Salida? = null,
    val llegada: Salida? = null,
    val segundos: Int? = null,
    val programada: Boolean? = null,
)

@Serializable
data class Salida(
    val programada: String? = null,
    val enAnden: String? = null,
)

@Serializable
data class NamedId(
    val id: Int? = null,
    val nombre: String? = null,
)

@Serializable
data class Servicio(
    val numero: Int? = null,
    val ramal: Ramal? = null,
    val gerencia: NamedId? = null,
    val cancelacion: JsonElement? = null,
    val oculto: Boolean? = null,
    val sentido: Int? = null,
    val tipo: Tipo? = null,
    val leyenda: String? = null,
    val location: TrainLocation? = null,
    val id: String? = null,
)

@Serializable
data class Ramal(
    val id: Int? = null,
    val nombre: String? = null,
    val tolerancia: Int? = null,
    val cabeceraFinal: NamedId? = null,
    val cabeceraInicial: NamedId? = null,
)

@Serializable
data class Tipo(
    val id: Int? = null,
    val nombre: String? = null,
    val programado: Boolean? = null,
)

@Serializable
data class TrainLocation(
    val lat: Double? = null,
    val long: Double? = null,
)

@Serializable
data class GerenciaDto(
    val id: Int? = null,
    val nombre: String? = null,
    val estado: EstadoDto? = null,
    val alerta: List<AlertaDto> = emptyList(),
)

@Serializable
data class EstadoDto(
    val id: Int? = null,
    val mensaje: String? = null,
    val color: String? = null,
)

@Serializable
data class AlertaDto(
    val id: Long? = null,
    @SerialName("linea_id") val lineaId: Int? = null,
    @SerialName("ramal_id") val ramalId: Int? = null,
    val titulo: String? = null,
    val contenido: String? = null,
)
