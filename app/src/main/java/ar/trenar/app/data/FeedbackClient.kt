package ar.trenar.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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

    /** @return true si el servidor aceptó el feedback. */
    suspend fun send(
        type: String,            // "feature" | "bug"
        title: String,
        description: String,
        contact: String? = null,
    ): Boolean = withContext(Dispatchers.IO) {
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

        runCatching {
            client.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
