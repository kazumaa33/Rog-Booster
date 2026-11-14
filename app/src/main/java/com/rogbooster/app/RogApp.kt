package com.rogbooster.app

import android.app.Application
import android.util.Log
import com.rogbooster.app.core.Prefs

/** Guarda el último error no controlado para mostrarlo al reabrir la app. */
class RogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                Prefs.putStrSync(this, "last_report", "CRASH en hilo ${t.name}:\n" + Log.getStackTraceString(e).take(3500))
            } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
    }
}
