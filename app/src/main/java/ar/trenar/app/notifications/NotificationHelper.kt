package ar.trenar.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ar.trenar.app.MainActivity
import ar.trenar.app.R

object NotificationHelper {

    const val CHANNEL_DELAYS = "delays"
    const val CHANNEL_LEAVE = "leave_now"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val delays = NotificationChannel(
            CHANNEL_DELAYS,
            context.getString(R.string.channel_delays_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_delays_desc) }
        val leave = NotificationChannel(
            CHANNEL_LEAVE,
            context.getString(R.string.channel_leave_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_leave_desc) }
        nm.createNotificationChannel(delays)
        nm.createNotificationChannel(leave)
    }

    fun canPost(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notifyDelay(
        context: Context,
        notifId: Int,
        title: String,
        text: String,
        stationId: Int,
        channel: String = CHANNEL_DELAYS,
    ) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_STATION_ID, stationId)
        }
        val pi = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_train)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (_: SecurityException) {
            // permission revoked between check and post
        }
    }
}
