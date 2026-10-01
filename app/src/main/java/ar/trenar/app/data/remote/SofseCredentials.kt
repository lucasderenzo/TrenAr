package ar.trenar.app.data.remote

import android.util.Base64
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Reproduces the credential scheme the SOFSE / Trenes Argentinos clients use to obtain a
 * short-lived JWT from `/auth/authorize`. This is the same "security through obscurity" login
 * every SOFSE client performs; it is implemented here so the app can talk to the official API
 * on the user's device, exactly like the official app does.
 *
 * username = base64("<yyyyMMdd><suffix>") using the Buenos Aires local date.
 * password = a fixed cipher/reverse/base64 pipeline over the username, URL-encoded.
 *
 * Note: the server validates the date against Argentina local time, so we use the AR zone
 * (the naive UTC-based clients fail during the late-night UTC window — we don't).
 */
object SofseCredentials {
    private val AR_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private const val SUFFIX = "sofse"

    // char -> [replacement for step 0, replacement for step 1]
    private val cipher: List<Pair<String, Array<String>>> = listOf(
        "a" to arrayOf("#t", "#j"),
        "e" to arrayOf("#x", "#p"),
        "i" to arrayOf("#f", "#w"),
        "o" to arrayOf("#l", "#8"),
        "u" to arrayOf("#7", "#0"),
        "=" to arrayOf("#g", "#v"),
    )

    data class Credentials(val username: String, val password: String)

    private fun b64(s: String): String =
        Base64.encodeToString(s.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun applyCipher(input: String, step: Int): String {
        var s = input
        for ((from, out) in cipher) s = s.replace(from, out[step])
        return s
    }

    /** Equivalent to JavaScript encodeURIComponent(). */
    private fun encodeUriComponent(s: String): String {
        val safe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()"
        val sb = StringBuilder()
        for (byte in s.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt() and 0xFF
            val ch = c.toChar()
            if (safe.indexOf(ch) >= 0) sb.append(ch)
            else sb.append('%').append("%02X".format(c))
        }
        return sb.toString()
    }

    private fun usernameFor(day: ZonedDateTime): String = b64(day.format(DATE_FMT) + SUFFIX)

    private fun encodePassword(username: String): String {
        var p = b64(username)
        p = applyCipher(p, 0)
        p = p.reversed()
        p = b64(p)
        p = applyCipher(p, 1)
        p = p.reversed()
        return encodeUriComponent(p)
    }

    /**
     * Candidate credentials: Buenos Aires "today" first, then adjacent days as a robust fallback
     * around midnight / clock skew.
     */
    fun candidates(now: ZonedDateTime = ZonedDateTime.now()): List<Credentials> {
        val today = now.withZoneSameInstant(AR_ZONE)
        return listOf(today, today.minusDays(1), today.plusDays(1)).map {
            val u = usernameFor(it)
            Credentials(u, encodePassword(u))
        }
    }
}
