package com.chisara.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.chisara.app.MainActivity
import com.chisara.app.R

/**
 * Posts the generic "blind" notification that replaces the intercepted one.
 * It reveals nothing about the sender or content — tapping it opens the app to guess.
 */
class BlindNotifier(private val context: Context) {

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        ensureChannel()
    }

    private fun ensureChannel() {
        val existing = manager.getNotificationChannel(NotificationConfig.BLIND_CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                NotificationConfig.BLIND_CHANNEL_ID,
                NotificationConfig.BLIND_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche cieche del gioco Chi Sarà"
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * @param eventId the NotificationEvent row id, used to derive a stable notification id
     *                and passed to the activity so it can deep-link to the guess screen.
     * @param arrivalTs arrival time, shown as "arrivato alle HH:MM".
     * @return the notification id we posted (stored on the event so it can be dismissed later).
     */
    fun postBlind(eventId: Long, sourcePackage: String, arrivalTs: Long): Int {
        val notificationId = NotificationConfig.BLIND_NOTIFICATION_BASE_ID + eventId.toInt()

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_GUESS
            putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationConfig.BLIND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📩 Nuovo messaggio misterioso")
            .setContentText("Tocca per indovinare chi te l'ha mandato…")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)
        return notificationId
    }

    fun cancelBlind(notificationId: Int) {
        manager.cancel(notificationId)
    }
}
