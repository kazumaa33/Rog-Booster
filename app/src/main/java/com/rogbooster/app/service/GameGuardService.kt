package com.rogbooster.app.service

import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.rogbooster.app.GameState
import com.rogbooster.app.core.OptimizationManager
import com.rogbooster.app.core.PermissionHelper
import com.rogbooster.app.core.SystemMonitor

/**
 * Servicio en primer plano que vigila la partida:
 *  - alertas dinámicas de temperatura + liberación automática de RAM/caché (anti thermal throttling)
 *  - detecta que el juego se cerró (requiere acceso de uso) y restaura el entorno
 */
class GameGuardService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastFg: String? = null
    private var awayTicks = 0
    private var lastAlertAt = 0L
    private var lastCleanAt = 0L

    private val tick = object : Runnable {
        override fun run() {
            try { checkThermal() } catch (_: Exception) {}
            try { checkGameAlive() } catch (_: Exception) {}
            handler.postDelayed(this, 2000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            OptimizationManager.stopGame(this)
            return START_NOT_STICKY
        }
        val stop = PendingIntent.getService(
            this, 1, Intent(this, GameGuardService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        if (!Notifier.startFg(this, 1001, Notifier.foreground(this, "Modo juego activo", stop))) {
            stopSelf(); return START_NOT_STICKY
        }
        lastFg = GameState.currentPackage
        awayTicks = 0
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    private fun checkThermal() {
        val temp = SystemMonitor.batteryTempC(this)
        val level = SystemMonitor.thermalLevel(temp)
        val now = System.currentTimeMillis()
        if (level >= SystemMonitor.Thermal.HOT && now - lastAlertAt > 60_000) {
            lastAlertAt = now
            val msg = if (level == SystemMonitor.Thermal.CRITICAL)
                "%.1f °C: riesgo de thermal throttling. Baja los gráficos o haz una pausa.".format(temp)
            else "%.1f °C: el dispositivo se está calentando.".format(temp)
            Notifier.alert(this, "⚠ Temperatura alta", msg)
        }
        // Liberación de RAM/caché automática (máx. cada 2 min) al entrar en zona caliente
        if (level >= SystemMonitor.Thermal.HOT && now - lastCleanAt > 120_000) {
            lastCleanAt = now
            Thread { OptimizationManager.clearMemory(this, GameState.currentPackage) }.start()
        }
    }

    private fun checkGameAlive() {
        if (!PermissionHelper.usageAccess(this)) return
        val fg = foregroundPackage() ?: lastFg
        lastFg = fg
        val game = GameState.currentPackage
        val away = fg != null && fg != game && fg != packageName
        awayTicks = if (away) awayTicks + 1 else 0
        if (awayTicks >= 10) OptimizationManager.stopGame(this)   // ~20 s fuera del juego
    }

    @Suppress("DEPRECATION")
    private fun foregroundPackage(): String? {
        val usm = getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - 60_000, end)
        val e = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = e.packageName
        }
        return last
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object { const val ACTION_STOP = "com.rogbooster.app.STOP" }
}
