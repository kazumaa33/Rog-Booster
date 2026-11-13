package com.rogbooster.app.core

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object PermissionHelper {
    fun notifListener(c: Context) =
        NotificationManagerCompat.getEnabledListenerPackages(c).contains(c.packageName)

    fun dnd(c: Context) = c.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    fun overlay(c: Context) = Settings.canDrawOverlays(c)
    fun writeSettings(c: Context) = Settings.System.canWrite(c)

    @Suppress("DEPRECATION")
    fun usageAccess(c: Context): Boolean {
        val ops = c.getSystemService(AppOpsManager::class.java)
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    fun intentNotifListener() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    fun intentDnd() = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    fun intentOverlay(c: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${c.packageName}"))
    fun intentWriteSettings(c: Context) =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${c.packageName}"))
    fun intentUsage() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
