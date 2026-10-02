package ar.trenar.app.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import ar.trenar.app.MainActivity
import ar.trenar.app.R
import ar.trenar.app.data.model.Arrival
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Foreground service that follows a station and keeps a live countdown notification. */
class TrainTrackService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }
        val stationId = intent?.getIntExtra(EXTRA_STATION_ID, -1) ?: -1
        val stationName = intent?.getStringExtra(EXTRA_STATION_NAME) ?: "Estación"
        if (stationId <= 0) {
            stopEverything()
            return START_NOT_STICKY
        }

        NotificationHelper.ensureChannels(this)
        _tracked.value = stationId
        startForegroundCompat(buildNotification(stationName, null, System.currentTimeMillis() / 1000))

        job?.cancel()
        job = scope.launch {
            ServiceLocator.init(applicationContext)
            var misses = 0
            while (isActive) {
                val next = runCatching { ServiceLocator.repository.nextArrival(stationId) }.getOrNull()
                val now = System.currentTimeMillis() / 1000
                post(buildNotification(stationName, next, now))
                val eta = next?.etaSecondsAt(now)
                if (next != null && eta != null && eta <= 20) {
                    post(buildArriving(stationName, next))
                    delay(25_000)
                    break
                }
                if (next == null) {
                    if (++misses >= 10) break
                } else {
                    misses = 0
                }
                delay(20_000)
            }
            stopEverything()
        }
        return START_STICKY
    }

    private fun buildNotification(stationName: String, next: Arrival?, nowSec: Long): Notification {
        val b = baseBuilder(stationName)
        if (next != null && next.etaSeconds != null && !next.cancelled) {
            val eta = next.etaSecondsAt(nowSec) ?: 0
            b.setContentText(detail(next))
            b.setWhen(System.currentTimeMillis() + eta * 1000L)
            b.setShowWhen(true)
            b.setUsesChronometer(true)
            b.setChronometerCountDown(true)
        } else {
            b.setShowWhen(false)
            b.setContentText(if (next == null) "Sin próximos trenes ahora" else "Servicio cancelado")
        }
        return b.build()
    }

    private fun buildArriving(stationName: String, next: Arrival): Notification =
        baseBuilder(stationName)
            .setShowWhen(false)
            .setContentText("¡Llegando! ${detail(next)}")
            .build()

    private fun baseBuilder(stationName: String): NotificationCompat.Builder {
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_STATION_ID, _tracked.value ?: -1)
        }
        val openPi = PendingIntent.getActivity(
            this, 1, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = Intent(this, TrainTrackService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(
            this, 2, stop,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_TRACK)
            .setSmallIcon(R.drawable.ic_stat_train)
            .setContentTitle(stationName)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPi)
            .addAction(0, "Dejar de seguir", stopPi)
    }

    private fun detail(next: Arrival): String = buildString {
        append("a ${next.destination}")
        next.platform?.let { append(" · andén $it") }
        next.delayMinutes()?.let { append(" · +$it min") }
    }

    private fun post(notification: Notification) {
        if (NotificationHelper.canPost(this)) {
            try {
                NotificationManagerCompat.from(this).notify(NOTIF_ID, notification)
            } catch (_: SecurityException) {
            }
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, notification, type)
    }

    private fun stopEverything() {
        job?.cancel()
        _tracked.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        job?.cancel()
        _tracked.value = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 42_001
        private const val ACTION_START = "ar.trenar.app.track.START"
        private const val ACTION_STOP = "ar.trenar.app.track.STOP"
        private const val EXTRA_STATION_ID = "station_id"
        private const val EXTRA_STATION_NAME = "station_name"

        private val _tracked = MutableStateFlow<Int?>(null)

        /** Station id currently being followed, or null. */
        val tracked: StateFlow<Int?> = _tracked.asStateFlow()

        fun start(context: Context, stationId: Int, stationName: String) {
            val i = Intent(context, TrainTrackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STATION_ID, stationId)
                putExtra(EXTRA_STATION_NAME, stationName)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            val i = Intent(context, TrainTrackService::class.java).apply { action = ACTION_STOP }
            androidx.core.content.ContextCompat.startForegroundService(context, i)
        }
    }
}
