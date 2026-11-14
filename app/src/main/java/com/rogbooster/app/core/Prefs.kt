package com.rogbooster.app.core

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("rog_booster", Context.MODE_PRIVATE)

    fun bool(c: Context, k: String, d: Boolean) = sp(c).getBoolean(k, d)
    fun putBool(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()
    fun int(c: Context, k: String, d: Int) = sp(c).getInt(k, d)
    fun putInt(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()
    fun str(c: Context, k: String, d: String) = sp(c).getString(k, d) ?: d
    fun putStrSync(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).commit()

    // Claves de configuración
    const val OVERLAY = "overlay_enabled"
    const val CROSSHAIR = "crosshair_enabled"
    const val BRIGHT_LOCK = "brightness_lock"
    const val BRIGHT_LEVEL = "brightness_level"      // 10..255
    const val POINTER_ON = "pointer_enabled"
    const val POINTER_LEVEL = "pointer_level"        // -7..7

    // Claves de estado previo (para restaurar al terminar la partida)
    const val S_FILTER = "saved_filter"
    const val S_MUSIC = "saved_music"
    const val S_RING = "saved_ring"
    const val S_RINGER = "saved_ringer"
    const val S_BRIGHT = "saved_brightness"
    const val S_BRIGHT_MODE = "saved_brightness_mode"
    const val S_POINTER = "saved_pointer"
}
