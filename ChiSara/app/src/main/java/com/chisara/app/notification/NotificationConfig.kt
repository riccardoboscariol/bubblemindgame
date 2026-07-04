package com.chisara.app.notification

/**
 * Which apps are part of the game, and the channel/id constants for our blind
 * replacement notifications. The tracked-package set is intentionally a single
 * place so it can later be made user-configurable from settings.
 */
object NotificationConfig {

    const val PKG_WHATSAPP = "com.whatsapp"
    const val PKG_TELEGRAM = "org.telegram.messenger"
    const val PKG_INSTAGRAM = "com.instagram.android"

    /** Default set of tracked packages (configurable later). */
    val trackedPackages: Set<String> = setOf(PKG_WHATSAPP, PKG_TELEGRAM, PKG_INSTAGRAM)

    /** Human-friendly app names for UI. */
    fun displayName(pkg: String): String = when (pkg) {
        PKG_WHATSAPP -> "WhatsApp"
        PKG_TELEGRAM -> "Telegram"
        PKG_INSTAGRAM -> "Instagram"
        else -> pkg
    }

    const val BLIND_CHANNEL_ID = "chisara_blind"
    const val BLIND_CHANNEL_NAME = "Messaggi misteriosi"

    /** Base id for replacement notifications; the event id offset keeps them distinct. */
    const val BLIND_NOTIFICATION_BASE_ID = 10_000
}
