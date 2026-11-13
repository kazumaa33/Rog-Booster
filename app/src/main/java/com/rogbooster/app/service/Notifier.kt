package com.rogbooster.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.rogbooster.app.MainActivity
import com.rogbooster.app.R

object Notifier {
    const val CH_SERVICE = "rog_service"
    const val CH_ALERT = "rog_alert"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_SERVICE, "Rog Booster activo", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Alertas térmicas", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun openApp(ctx: Context) = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun foreground(ctx: Context, text: String, stopIntent: PendingIntent? = null): Notification {
        ensureChannels(ctx)
        val b = NotificationCompat.Builder(ctx, CH_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_rocket)
            .setContentTitle("Rog Booster")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openApp(ctx))
        if (stopIntent != null) b.addAction(0, "Detener", stopIntent)
        return b.build()
    }

    fun alert(ctx: Context, title: String, text: String) {
        ensureChannels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_ALERT)
            .setSmallIcon(R.drawable.ic_stat_rocket)
            .setContentTitle(title).setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setContentIntent(openApp(ctx)).build()
        ctx.getSystemService(NotificationManager::class.java).notify(2001, n)
    }
}
