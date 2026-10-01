package ar.trenar.app.data.remote

import android.content.Context
import android.util.Base64
import org.json.JSONObject

/** Persists and validates the bearer JWT. Reads are synchronous so OkHttp interceptors can use it. */
class TokenStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("trenar_auth", Context.MODE_PRIVATE)

    @Volatile
    private var cached: String? = prefs.getString(KEY, null)

    val token: String? get() = cached

    fun save(token: String) {
        cached = token
        prefs.edit().putString(KEY, token).apply()
    }

    fun clear() {
        cached = null
        prefs.edit().remove(KEY).apply()
    }

    /** True when there is no token, it is malformed, or it expires within [skewSeconds]. */
    fun isExpired(token: String? = cached, skewSeconds: Long = 120): Boolean {
        val t = token ?: return true
        return try {
            val parts = t.split(".")
            if (parts.size < 2) return true
            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charsets.UTF_8,
            )
            val exp = JSONObject(payload).optLong("exp", 0L)
            val now = System.currentTimeMillis() / 1000
            exp == 0L || now > (exp - skewSeconds)
        } catch (e: Exception) {
            true
        }
    }

    companion object {
        private const val KEY = "jwt"
    }
}
