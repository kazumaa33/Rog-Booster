package com.rogbooster.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.rogbooster.app.GameState

/** Cancela al instante cualquier notificación entrante mientras hay una partida activa. */
class GameNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!GameState.isGameRunning) return
        if (sbn.packageName == packageName) return   // no bloquear nuestras propias alertas
        if (!sbn.isClearable) return                 // las "ongoing" no se pueden descartar
        try {
            cancelNotification(sbn.key)
        } catch (_: SecurityException) { /* listener aún no enlazado */ }
    }
}
