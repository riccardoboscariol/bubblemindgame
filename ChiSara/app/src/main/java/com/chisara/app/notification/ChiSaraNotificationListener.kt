package com.chisara.app.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chisara.app.data.repository.GameRepository
import com.chisara.app.data.settings.AppSettings
import com.chisara.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Intercepts notifications from the tracked messaging apps *before* the user reads
 * them, hides the real content, and posts a generic "blind" notification instead.
 *
 * Flow in [onNotificationPosted]:
 *  1. Ignore anything that isn't a tracked app, is our own notification, is a
 *     summary/ongoing notification, or has no identifiable sender.
 *  2. Group messages are skipped unless the user enabled them in settings (and only
 *     when the sender within the group is actually known).
 *  3. If the message body is already readable (the app has previews enabled), drop
 *     it from the game — the user would see it in the shade anyway, so it isn't
 *     "guessable".
 *  4. Otherwise store the true sender, cancel the original, and post the blind one.
 */
class ChiSaraNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { GameRepository(applicationContext) }
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    /** Recently handled notification keys → timestamp, to dedupe rapid re-posts. */
    private val recentlyHandled = HashMap<String, Long>()

    /**
     * Cached settings kept in sync with DataStore, so the hot [onNotificationPosted]
     * path reads tracked packages / group policy synchronously.
     */
    @Volatile
    private var settings: AppSettings = AppSettings()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return
        if (pkg !in settings.trackedPackages) return

        val notification = sbn.notification ?: return
        if (shouldSkip(sbn, notification)) return

        val now = System.currentTimeMillis()
        val extras = notification.extras
        val msg = extractMessage(notification, extras)

        val sender = msg.sender
        if (sender.isNullOrBlank() || isGenericSender(sender, pkg)) {
            // No identifiable person → nothing to guess. Leave the notification alone.
            return
        }

        if (msg.isGroup) {
            if (!settings.includeGroups) return
            // In a group we can only play if we know WHO wrote (a distinct person,
            // not just the group name).
            if (msg.groupName == null || sender.equals(msg.groupName, ignoreCase = true)) return
        }

        val body = extractBody(extras)
        if (isReadablePreview(body) && !settings.playWithPreviewOn) {
            // Strict mode: previews are on, the user could read it in the shade — skip.
            // In relaxed mode we keep it and hide the text ourselves via the blind notification.
            return
        }

        // Always cancel the original — including on re-posts/updates of the same key
        // (apps like Telegram repost the same notification several times). If we deduped
        // *before* cancelling, a re-post would linger in the shade showing the sender.
        try {
            cancelNotification(sbn.key)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not cancel original notification", t)
        }

        // Dedupe only the game event: one round per key per window, even though we keep
        // cancelling every re-post above.
        val lastHandled = recentlyHandled[sbn.key]
        if (lastHandled != null && now - lastHandled < DEDUPE_WINDOW_MS) return
        recentlyHandled[sbn.key] = now
        pruneRecent(now)

        scope.launch {
            try {
                repository.recordInterceptedNotification(
                    senderName = sender,
                    sourcePackage = pkg,
                    arrivalTs = now,
                    groupName = if (msg.isGroup) msg.groupName else null
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to record intercepted notification", t)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
        // Keep the settings cache in sync with the user's choices.
        settingsRepository.settings
            .onEach { settings = it }
            .launchIn(scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
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

    /** Sender + group context extracted from a notification. */
    private data class ExtractedMessage(
        val sender: String?,
        val groupName: String?,
        val isGroup: Boolean
    )

    /**
     * Resolves the sender and whether this is a group message.
     * MessagingStyle is the reliable source: the last message's Person is the real
     * sender, and a non-blank conversationTitle (or the group flag) marks a group.
     */
    private fun extractMessage(
        notification: Notification,
        extras: android.os.Bundle
    ): ExtractedMessage {
        val style = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)
        val person = style?.messages?.lastOrNull()?.person?.name?.toString()
        val conversationTitle = style?.conversationTitle?.toString()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        val groupFlag = extras.getBoolean("android.isGroupConversation", false)
        val isGroup = groupFlag || (!conversationTitle.isNullOrBlank() && !person.isNullOrBlank())

        val sender = when {
            !person.isNullOrBlank() -> person
            !isGroup && !conversationTitle.isNullOrBlank() -> conversationTitle
            else -> title
        }
        val groupName = if (isGroup) (conversationTitle ?: title) else null
        return ExtractedMessage(sender = sender, groupName = groupName, isGroup = isGroup)
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
