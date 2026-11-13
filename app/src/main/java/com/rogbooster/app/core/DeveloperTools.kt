package com.rogbooster.app.core

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

object DeveloperTools {
    private fun enabled(c: Context) =
        Settings.Global.getInt(c.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

    /** Abre Opciones de desarrollador (donde está "Ancho más pequeño"). */
    fun openDeveloperOptions(c: Context) {
        val i = if (enabled(c)) Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        else {
            Toast.makeText(c, "Activa las opciones: toca 7 veces «Número de compilación»", Toast.LENGTH_LONG).show()
            Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
        }
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Densidad (dpi) necesaria para lograr un "Ancho más pequeño" objetivo. */
    fun densityFor(c: Context, targetSwDp: Int): Int {
        val m = c.resources.displayMetrics
        val shortPx = minOf(m.widthPixels, m.heightPixels)
        return (160f * shortPx / targetSwDp).toInt()
    }

    fun currentSmallestWidthDp(c: Context) = c.resources.configuration.smallestScreenWidthDp
}
