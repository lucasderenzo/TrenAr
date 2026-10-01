package ar.trenar.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ar.trenar.app.MainActivity
import ar.trenar.app.R
import ar.trenar.app.di.ServiceLocator

/** Renders the pinned station's next trains into all placed widgets. */
object WidgetUpdater {

    suspend fun updateAll(context: Context) {
        val app = context.applicationContext
        val mgr = AppWidgetManager.getInstance(app)
        val ids = mgr.getAppWidgetIds(ComponentName(app, NextTrainWidgetProvider::class.java))
        if (ids.isEmpty()) return

        ServiceLocator.init(app)
        val prefs = ServiceLocator.prefs
        val pinned = prefs.pinnedNow()

        val views = RemoteViews(app.packageName, R.layout.widget_next_train)

        if (pinned == null) {
            views.setTextViewText(R.id.widget_title, app.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_subtitle, app.getString(R.string.widget_empty))
            views.setTextViewText(R.id.widget_row_0, "")
            views.setTextViewText(R.id.widget_row_1, "")
            views.setTextViewText(R.id.widget_row_2, "")
        } else {
            val board = runCatching { ServiceLocator.repository.board(pinned, cantidad = 6) }.getOrNull()
            val name = board?.station?.name
                ?: ServiceLocator.repository.station(pinned)?.name
                ?: "Estación"
            views.setTextViewText(R.id.widget_title, name)
            val now = System.currentTimeMillis() / 1000
            if (board == null || board.arrivals.isEmpty()) {
                views.setTextViewText(R.id.widget_subtitle, "Sin datos ahora")
            } else {
                views.setTextViewText(R.id.widget_subtitle, "Actualizado recién")
            }
            val rowIds = intArrayOf(R.id.widget_row_0, R.id.widget_row_1, R.id.widget_row_2)
            val arrivals = board?.arrivals ?: emptyList()
            for (i in rowIds.indices) {
                val text = arrivals.getOrNull(i)?.let { a ->
                    val eta = a.etaSecondsAt(now)
                    val etaText = when {
                        a.cancelled -> "cancelado"
                        eta == null -> "s/d"
                        eta < 60 -> "llegando"
                        else -> "${eta / 60} min"
                    }
                    "${a.destination} · $etaText"
                } ?: ""
                views.setTextViewText(rowIds[i], text)
            }
        }

        val openIntent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (pinned != null) putExtra(MainActivity.EXTRA_STATION_ID, pinned)
        }
        val pi = PendingIntent.getActivity(
            app, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pi)

        for (id in ids) {
            mgr.updateAppWidget(id, views)
        }
    }
}
