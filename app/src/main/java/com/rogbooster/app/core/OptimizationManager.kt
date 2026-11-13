package com.rogbooster.app.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.core.content.ContextCompat
import com.rogbooster.app.GameState
import com.rogbooster.app.service.FloatingWidgetService
import com.rogbooster.app.service.GameGuardService

object OptimizationManager {

    /** Cierra procesos en segundo plano y vacía la caché propia. Devuelve MB liberados (aprox.). */
    fun clearMemory(ctx: Context, exceptPkg: String? = null): Long {
        val before = SystemMonitor.ram(ctx).freeMb
        val am = ctx.getSystemService(ActivityManager::class.java)
        ctx.packageManager.getInstalledApplications(0).forEach { ai ->
            val isSystem = ai.flags and ApplicationInfo.FLAG_SYSTEM != 0
            if (!isSystem && ai.packageName != ctx.packageName && ai.packageName != exceptPkg) {
                am.killBackgroundProcesses(ai.packageName)
            }
        }
        ctx.cacheDir.deleteRecursively()
        ctx.externalCacheDir?.deleteRecursively()
        Thread.sleep(400)
        return (SystemMonitor.ram(ctx).freeMb - before).coerceAtLeast(0)
    }

    /** Aplica todas las optimizaciones y abre el juego. Devuelve false si no se puede lanzar. */
    fun startGame(ctx: Context, pkg: String): Boolean {
        val launch = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        if (!GameState.isGameRunning) {            // evita sobrescribir el estado a restaurar
            clearMemory(ctx, pkg)
            DndController.enable(ctx)
            AudioController.apply(ctx)
            BrightnessController.apply(ctx)
            PointerController.apply(ctx)
        }
        GameState.currentPackage = pkg
        GameState.isGameRunning = true

        ContextCompat.startForegroundService(ctx, Intent(ctx, GameGuardService::class.java))
        if (Prefs.bool(ctx, Prefs.OVERLAY, true) && PermissionHelper.overlay(ctx)) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, FloatingWidgetService::class.java))
        }
        ctx.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    /** Restaura el entorno original y detiene los servicios. */
    fun stopGame(ctx: Context) {
        if (GameState.isGameRunning) {
            DndController.restore(ctx)
            AudioController.restore(ctx)
            BrightnessController.restore(ctx)
            PointerController.restore(ctx)
        }
        GameState.isGameRunning = false
        GameState.currentPackage = null
        ctx.stopService(Intent(ctx, FloatingWidgetService::class.java))
        ctx.stopService(Intent(ctx, GameGuardService::class.java))
    }
}
