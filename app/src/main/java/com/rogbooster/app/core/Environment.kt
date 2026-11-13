package com.rogbooster.app.core

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.provider.Settings

/** Modo "No Molestar" total. Requiere acceso a la política de notificaciones. */
object DndController {
    fun enable(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return
        Prefs.putInt(ctx, Prefs.S_FILTER, nm.currentInterruptionFilter)
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
    }

    fun restore(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return
        nm.setInterruptionFilter(
            Prefs.int(ctx, Prefs.S_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        )
    }
}

/** Multimedia al 100% y llamadas/tonos silenciados durante la partida. */
object AudioController {
    fun apply(ctx: Context) {
        val am = ctx.getSystemService(AudioManager::class.java)
        Prefs.putInt(ctx, Prefs.S_MUSIC, am.getStreamVolume(AudioManager.STREAM_MUSIC))
        Prefs.putInt(ctx, Prefs.S_RING, am.getStreamVolume(AudioManager.STREAM_RING))
        Prefs.putInt(ctx, Prefs.S_RINGER, am.ringerMode)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
        try { am.setStreamVolume(AudioManager.STREAM_RING, 0, 0) } catch (_: SecurityException) {}
    }

    fun restore(ctx: Context) {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, Prefs.int(ctx, Prefs.S_MUSIC, 8), 0)
        try {
            am.ringerMode = Prefs.int(ctx, Prefs.S_RINGER, AudioManager.RINGER_MODE_NORMAL)
            am.setStreamVolume(AudioManager.STREAM_RING, Prefs.int(ctx, Prefs.S_RING, 4), 0)
        } catch (_: SecurityException) {}
    }
}

/** Brillo fijo. Requiere WRITE_SETTINGS (Settings.System.canWrite). */
object BrightnessController {
    fun apply(ctx: Context) {
        if (!Prefs.bool(ctx, Prefs.BRIGHT_LOCK, false) || !Settings.System.canWrite(ctx)) return
        val cr = ctx.contentResolver
        Prefs.putInt(ctx, Prefs.S_BRIGHT, Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128))
        Prefs.putInt(ctx, Prefs.S_BRIGHT_MODE, Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 1))
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, Prefs.int(ctx, Prefs.BRIGHT_LEVEL, 200))
    }

    fun restore(ctx: Context) {
        if (!Prefs.bool(ctx, Prefs.BRIGHT_LOCK, false) || !Settings.System.canWrite(ctx)) return
        val cr = ctx.contentResolver
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, Prefs.int(ctx, Prefs.S_BRIGHT, 128))
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Prefs.int(ctx, Prefs.S_BRIGHT_MODE, 1))
    }
}

/** Velocidad del puntero del sistema (Settings.System "pointer_speed", rango -7..7). */
object PointerController {
    private const val KEY = "pointer_speed" // = Settings.System.POINTER_SPEED (API oculta, usamos la cadena)

    fun apply(ctx: Context) {
        if (!Prefs.bool(ctx, Prefs.POINTER_ON, false) || !Settings.System.canWrite(ctx)) return
        val cr = ctx.contentResolver
        Prefs.putInt(ctx, Prefs.S_POINTER, Settings.System.getInt(cr, KEY, 0))
        Settings.System.putInt(cr, KEY, Prefs.int(ctx, Prefs.POINTER_LEVEL, 0).coerceIn(-7, 7))
    }

    fun restore(ctx: Context) {
        if (!Prefs.bool(ctx, Prefs.POINTER_ON, false) || !Settings.System.canWrite(ctx)) return
        Settings.System.putInt(ctx.contentResolver, KEY, Prefs.int(ctx, Prefs.S_POINTER, 0))
    }
}
