package ar.trenar.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** Resultado de enviar feedback, para que la UI pueda explicar por qué falló. */
sealed interface FeedbackResult {
    data object Success : FeedbackResult
    /** Sin conexión o servidor inalcanzable (DNS, conexión, timeout, TLS; p. ej. una red que lo bloquea). */
    data object Network : FeedbackResult
    /** HTTP 429: demasiados envíos seguidos. */
    data object RateLimited : FeedbackResult
    /** Cualquier otro HTTP no exitoso (5xx, 4xx) o error inesperado. */
    data object Server : FeedbackResult

    companion object {
        fun fromHttpCode(code: Int): FeedbackResult = when {
            code in 200..299 -> Success
            code == 429 -> RateLimited
            else -> Server
        }

        fun fromThrowable(t: Throwable): FeedbackResult = when (t) {
            is CancellationException -> throw t
            is IOException -> Network
            else -> Server
        }
    }
}

/**
 * Envía sugerencias/reportes de los usuarios al endpoint propio (en el VPS), que crea
 * la tarjeta en Trello con el token del lado del servidor. El token de Trello NUNCA viaja
 * en la app; sólo un secret de app que reduce el spam (el filtrado real es el triage humano).
 */
object FeedbackClient {

    private const val URL = "https://13-140-233-51.sslip.io/feedback"
    private const val APP_SECRET = "fb5cdadb1aa95d362da2fc643cadd8310f88122f19758252"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** @return [FeedbackResult.Success] si el servidor aceptó el feedback, o la causa del fallo. */
    suspend fun send(
        type: String,            // "feature" | "bug"
        title: String,
        description: String,
        contact: String? = null,
    ): FeedbackResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("type", type)
            put("title", title)
            put("description", description)
            if (!contact.isNullOrBlank()) put("contact", contact)
        }.toString()

        val req = Request.Builder()
            .url(URL)
            .addHeader("x-app-secret", APP_SECRET)
            .addHeader("content-type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(req).execute().use { FeedbackResult.fromHttpCode(it.code) }
        } catch (e: Exception) {
            FeedbackResult.fromThrowable(e)
        }
    }
}
