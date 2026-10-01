package ar.trenar.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ar.trenar.app.data.model.DelayStatus
import ar.trenar.app.di.ServiceLocator
import ar.trenar.app.widget.WidgetUpdater
import java.util.concurrent.TimeUnit

/** Periodically checks favorite stations and notifies on cancellations / major delays. */
class DelayWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        ServiceLocator.init(app)
        NotificationHelper.ensureChannels(app)

        val prefs = ServiceLocator.prefs
        if (!prefs.notifyEnabledNow()) {
            WidgetUpdater.updateAll(app)
            return Result.success()
        }

        val favorites = prefs.favoritesNow()
        val repo = ServiceLocator.repository

        for (stationId in favorites) {
            val board = runCatching { repo.board(stationId, cantidad = 8) }.getOrNull() ?: continue
            val stationName = board.station?.name ?: "tu estación"
            for (arrival in board.arrivals.take(6)) {
                when (arrival.status()) {
                    DelayStatus.CANCELLED -> NotificationHelper.notifyDelay(
                        context = app,
                        notifId = ("c" + arrival.serviceId).hashCode(),
                        title = "Tren cancelado · $stationName",
                        text = "${arrival.destination} (${arrival.line}) fue cancelado.",
                        stationId = stationId,
                    )
                    DelayStatus.MAJOR -> {
                        val min = arrival.delayMinutes() ?: continue
                        val platform = arrival.platform?.let { " · andén $it" } ?: ""
                        NotificationHelper.notifyDelay(
                            context = app,
                            notifId = ("d" + arrival.serviceId).hashCode(),
                            title = "Demora en ${arrival.line} · $stationName",
                            text = "${arrival.destination}: +$min min$platform.",
                            stationId = stationId,
                        )
                    }
                    else -> Unit
                }
            }
        }

        WidgetUpdater.updateAll(app)
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "trenar_delay_poll"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DelayWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
