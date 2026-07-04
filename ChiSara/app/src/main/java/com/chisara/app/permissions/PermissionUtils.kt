package com.chisara.app.permissions

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Both permissions this app relies on are *special access* permissions that the
 * user must grant from a system Settings screen — they are NOT runtime
 * permissions and cannot be requested with the normal permission dialog.
 * These helpers check the current state and build the deep-link intents.
 */
object PermissionUtils {

    /** True if the user has granted "Notification access" to our listener service. */
    fun hasNotificationAccess(context: Context): Boolean {
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabled.contains(context.packageName)
    }

    /**
     * Intent to the "Notification access" settings screen. On Android 11+ we can
     * deep-link straight to our own component's toggle; older versions open the list.
     */
    fun notificationAccessIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val component = ComponentName(
                context,
                "com.chisara.app.notification.ChiSaraNotificationListener"
            )
            val fragmentArg = ":settings:fragment_args_key"
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
                putExtra(fragmentArg, component.flattenToString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** True if the user has granted "Usage access" (PACKAGE_USAGE_STATS) to this app. */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Intent to the "Usage access" settings screen (targets our app when supported). */
    fun usageAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            // Not all OEMs honour the package Uri here, but it helps highlight our app when they do.
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** True if the app may post notifications (Android 13+ runtime permission). */
    fun hasPostNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
