package com.chisara.app.notification

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chisara.app.data.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Intercepts notifications from the tracked messaging apps *before* the user reads
 * them, hides the real content, and posts a generic "blind" notification instead.
 *
 * Flow in [onNotificationPosted]:
 *  1. Ignore anything that isn't a tracked app, is our own notification, is a
 *     summary/ongoing/group notification, or has no identifiable sender.
 *  2. If the message body is already readable (the app has previews enabled), drop
 *     it from the game — the user would see it in the shade anyway, so it isn't
 *     "guessable".
 *  3. Otherwise store the true sender, cancel the original, and post the blind one.
 */
class ChiSaraNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { GameRepository(applicationContext) }

    /** Recently handled notification keys → timestamp, to dedupe rapid re-posts. */
    private val recentlyHandled = HashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return
        if (pkg !in NotificationConfig.trackedPackages) return

        val notification = sbn.notification ?: return
        if (shouldSkip(sbn, notification)) return

        // Dedupe: WhatsApp/Telegram update the same key many times (typing, "N messages").
        val now = System.currentTimeMillis()
        val lastHandled = recentlyHandled[sbn.key]
        if (lastHandled != null && now - lastHandled < DEDUPE_WINDOW_MS) return

        val extras = notification.extras
        val sender = extractSender(notification, extras, pkg)
        if (sender.isNullOrBlank() || isGenericSender(sender, pkg)) {
            // No identifiable person → nothing to guess. Leave the notification alone.
            return
        }

        if (isGroupConversation(extras, sender)) {
            // Group messages are out of scope for the MVP.
            return
        }

        val body = extractBody(extras)
        if (isReadablePreview(body)) {
            // Previews are on: the user can already read it. Not part of the game.
            return
        }

        recentlyHandled[sbn.key] = now
        pruneRecent(now)

        // Replace: cancel the original, then record + post the blind notification.
        try {
            cancelNotification(sbn.key)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not cancel original notification", t)
        }

        scope.launch {
            try {
                repository.recordInterceptedNotification(
                    senderName = sender,
                    sourcePackage = pkg,
                    arrivalTs = now
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to record intercepted notification", t)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
    }

    // ---- Extraction helpers -----------------------------------------------------

    private fun shouldSkip(sbn: StatusBarNotification, notification: Notification): Boolean {
        val flags = notification.flags
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return true
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return true
        if (!sbn.isClearable) return true
        // Skip our own blind channel just in case.
        if (notification.channelId == NotificationConfig.BLIND_CHANNEL_ID) return true
        return false
    }

    /** The sender: prefer the MessagingStyle Person, fall back to the notification title. */
    private fun extractSender(
        notification: Notification,
        extras: android.os.Bundle,
        pkg: String
    ): String? {
        // MessagingStyle carries the most reliable per-message sender.
        val messagingSender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
                ?.messages?.lastOrNull()?.person?.name?.toString()
        } else null

        if (!messagingSender.isNullOrBlank()) return messagingSender

        val conversationTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
                ?.conversationTitle?.toString()
        } else null
        if (!conversationTitle.isNullOrBlank()) return conversationTitle

        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
    }

    private fun extractBody(extras: android.os.Bundle): String? {
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!bigText.isNullOrBlank()) return bigText
        return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    }

    /**
     * A sender that equals the app name (e.g. WhatsApp posts title "WhatsApp" when its
     * own preview is off) tells us nothing — treat it as unidentifiable.
     */
    private fun isGenericSender(sender: String, pkg: String): Boolean {
        val s = sender.trim().lowercase()
        return s == NotificationConfig.displayName(pkg).lowercase() ||
            s == "whatsapp" || s == "telegram" || s == "instagram" ||
            s.matches(Regex("\\d+ (nuovi )?messaggi.*")) // "3 messaggi", "3 new messages"
    }

    private fun isGroupConversation(extras: android.os.Bundle, sender: String): Boolean {
        if (extras.getBoolean("android.isGroupConversation", false)) return true
        // WhatsApp groups render the title as "Group name" and put "Sender: text" in body;
        // Telegram uses "Sender @ Group". A ':' or '@' in the title is a decent group signal.
        return sender.contains("@") && sender.contains(":")
    }

    /**
     * The body is a "readable preview" if it contains actual message text rather than
     * a generic placeholder that the source app shows when previews are disabled.
     */
    private fun isReadablePreview(body: String?): Boolean {
        if (body.isNullOrBlank()) return false
        val b = body.trim().lowercase()
        val placeholders = setOf(
            "messaggio", "nuovo messaggio", "message", "new message",
            "ti ha inviato un messaggio", "sent you a message",
            "messaggio ricevuto", "🔒 messaggio"
        )
        if (b in placeholders) return false
        if (b.matches(Regex("\\d+ (nuovi )?messaggi.*"))) return false
        // Any remaining non-trivial text means the user can read it in the shade.
        return b.length > 1
    }

    private fun pruneRecent(now: Long) {
        val it = recentlyHandled.entries.iterator()
        while (it.hasNext()) {
            if (now - it.next().value > DEDUPE_WINDOW_MS * 4) it.remove()
        }
    }

    private companion object {
        const val TAG = "ChiSaraListener"
        const val DEDUPE_WINDOW_MS = 4_000L
    }
}
