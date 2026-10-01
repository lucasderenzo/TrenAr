package ar.trenar.app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
private data class AuthReq(val username: String, val password: String)

@Serializable
private data class AuthResp(val token: String? = null, val message: String? = null)

/** Fetches and caches the SOFSE bearer token, retrying across credential candidates. */
class SofseAuthenticator(
    private val baseUrl: String,
    private val tokenStore: TokenStore,
) {
    private val bare = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val lock = Any()

    /** Returns a valid token, fetching a new one only if missing/expired. */
    fun ensureToken(): String? = synchronized(lock) {
        val current = tokenStore.token
        if (current != null && !tokenStore.isExpired(current)) current else fetchNew()
    }

    /** Forces a fresh token (used after a 401/403). */
    fun refresh(): String? = synchronized(lock) { fetchNew() }

    private fun fetchNew(): String? {
        for (cred in SofseCredentials.candidates()) {
            val payload = json.encodeToString(
                AuthReq.serializer(),
                AuthReq(cred.username, cred.password),
            )
            val req = Request.Builder()
                .url("$baseUrl/auth/authorize")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            try {
                bare.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        val token = runCatching {
                            json.decodeFromString(AuthResp.serializer(), text).token
                        }.getOrNull()
                        if (!token.isNullOrBlank()) {
                            tokenStore.save(token)
                            return token
                        }
                    }
                }
            } catch (_: IOException) {
                // network error — try the next candidate / give up
            }
        }
        return null
    }
}

/** Attaches the bearer token and transparently re-authenticates once on 401/403. */
class AuthInterceptor(private val auth: SofseAuthenticator) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.encodedPath.contains("/auth/authorize")) {
            return chain.proceed(original)
        }
        val token = auth.ensureToken()
        val firstReq = if (token != null) {
            original.newBuilder().header("Authorization", token).build()
        } else {
            original
        }
        var response = chain.proceed(firstReq)
        if (response.code == 401 || response.code == 403) {
            response.close()
            val fresh = auth.refresh()
            val retry = if (fresh != null) {
                original.newBuilder().header("Authorization", fresh).build()
            } else {
                original
            }
            response = chain.proceed(retry)
        }
        return response
    }
}
