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

    data class StartResult(val launched: Boolean, val errors: List<String>)

    private fun step(name: String, errors: MutableList<String>, block: () -> Unit) {
        try { block() } catch (e: Throwable) { errors += "$name: ${e.javaClass.simpleName} ${e.message ?: ""}" }
    }

    /** Aplica las optimizaciones (cada una aislada) y abre el juego aunque alguna falle. */
    fun startGame(ctx: Context, pkg: String): StartResult {
        val errors = mutableListOf<String>()
        val launch = ctx.packageManager.getLaunchIntentForPackage(pkg)
            ?: return StartResult(false, listOf("El juego no tiene actividad de lanzamiento"))
        if (!GameState.isGameRunning) {            // evita sobrescribir el estado a restaurar
            step("Limpiar RAM", errors) { clearMemory(ctx, pkg) }
            step("No molestar", errors) { DndController.enable(ctx) }
            step("Audio", errors) { AudioController.apply(ctx) }
            step("Brillo", errors) { BrightnessController.apply(ctx) }
            step("Puntero", errors) { PointerController.apply(ctx) }
        }
        GameState.currentPackage = pkg
        GameState.isGameRunning = true

        step("Vigilante", errors) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, GameGuardService::class.java))
        }
        step("Overlay", errors) {
            if (Prefs.bool(ctx, Prefs.OVERLAY, true) && PermissionHelper.overlay(ctx)) {
                ContextCompat.startForegroundService(ctx, Intent(ctx, FloatingWidgetService::class.java))
            }
        }
        var launched = false
        step("Abrir juego", errors) {
            ctx.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); launched = true
        }
        if (errors.isNotEmpty())
            Prefs.putStrSync(ctx, "last_report", "Optimizaciones que fallaron:\n" + errors.joinToString("\n"))
        return StartResult(launched, errors)
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
