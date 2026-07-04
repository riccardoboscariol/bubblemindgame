package com.chisara.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService

/**
 * After a reboot (or an app update) the system may not have re-bound our listener
 * yet. We ask it to rebind so interception resumes without the user reopening the app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                NotificationListenerService.requestRebind(
                    android.content.ComponentName(context, ChiSaraNotificationListener::class.java)
                )
            }
        }
    }
}
