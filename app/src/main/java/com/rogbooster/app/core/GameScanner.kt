package com.rogbooster.app.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.rogbooster.app.model.GameApp

object GameScanner {
    /** Respaldo para juegos que no declaran CATEGORY_GAME en su manifest. */
    private val knownGames = setOf(
        "com.tencent.ig", "com.pubg.imobile", "com.activision.callofduty.shooter",
        "com.dts.freefireth", "com.dts.freefiremax", "com.mojang.minecraftpe",
        "com.supercell.clashofclans", "com.supercell.brawlstars", "com.miHoYo.GenshinImpact",
        "com.roblox.client", "com.garena.game.codm", "com.epicgames.fortnite"
    )

    fun scan(ctx: Context): List<GameApp> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
            val ai = ri.activityInfo.applicationInfo
            if (ai.packageName == ctx.packageName) return@mapNotNull null
            val isGame = ai.category == ApplicationInfo.CATEGORY_GAME || ai.packageName in knownGames
            if (!isGame) null else GameApp(ri.loadLabel(pm).toString(), ai.packageName, ri.loadIcon(pm))
        }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
    }
}
