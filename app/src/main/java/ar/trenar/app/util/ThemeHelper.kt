package ar.trenar.app.util

import android.content.Context
import android.content.res.Configuration

/**
 * Per-app theme override (Sistema / Claro / Oscuro), stored in SharedPreferences so it can be read
 * synchronously in attachBaseContext — that way the first frame (window background, status bar
 * icons and Compose's isSystemInDarkTheme) already uses the chosen theme, with no flash.
 */
object ThemeHelper {
    private const val PREFS = "theme_prefs"
    private const val KEY = "theme_mode"

    const val SYSTEM = ""
    const val LIGHT = "light"
    const val DARK = "dark"

    /** [SYSTEM], [LIGHT] or [DARK]. */
    fun getMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun setMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode).apply()
    }

    /** Wraps a context so its resources (and night-mode qualifiers) resolve in the chosen theme. */
    fun wrap(context: Context): Context {
        val night = when (getMode(context)) {
            LIGHT -> Configuration.UI_MODE_NIGHT_NO
            DARK -> Configuration.UI_MODE_NIGHT_YES
            else -> return context
        }
        val config = Configuration(context.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        return context.createConfigurationContext(config)
    }
}
