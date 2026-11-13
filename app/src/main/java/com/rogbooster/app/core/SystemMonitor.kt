package com.rogbooster.app.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object SystemMonitor {
    data class Ram(val freeMb: Long, val totalMb: Long) {
        val usedPercent get() = if (totalMb == 0L) 0 else (100 - freeMb * 100 / totalMb).toInt()
    }

    enum class Thermal { OK, WARM, HOT, CRITICAL }

    fun ram(ctx: Context): Ram {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return Ram(mi.availMem / 1_048_576, mi.totalMem / 1_048_576)
    }

    /** Temperatura de la batería en °C (BatteryManager, intent "sticky"). */
    fun batteryTempC(ctx: Context): Float {
        val i: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val t = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return if (t < 0) -1f else t / 10f
    }

    fun thermalLevel(tempC: Float) = when {
        tempC >= 45f -> Thermal.CRITICAL
        tempC >= 42f -> Thermal.HOT
        tempC >= 38f -> Thermal.WARM
        else -> Thermal.OK
    }
}
