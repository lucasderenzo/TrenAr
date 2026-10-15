package ar.trenar.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Per-app language override, stored in SharedPreferences (read synchronously in attachBaseContext).
 * Empty tag = follow the system language.
 */
object LocaleHelper {
    private const val PREFS = "locale_prefs"
    private const val KEY = "app_lang"

    /** "" (system), "es", "en" or "pt". */
    fun getLang(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

    fun setLang(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    /** Wraps a context so its resources resolve in the chosen language. */
    fun wrap(context: Context): Context {
        val lang = getLang(context)
        if (lang.isEmpty()) return context
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
