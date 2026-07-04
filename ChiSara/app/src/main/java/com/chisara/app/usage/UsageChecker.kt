package com.chisara.app.usage

import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Anti-peek check. Uses [UsageStatsManager] to decide whether the user opened the
 * source app (e.g. WhatsApp) between the moment we intercepted the notification and
 * now. If they did, they most likely saw the real message, so the round doesn't count.
 */
class UsageChecker(context: Context) {

    private val usageStatsManager =
        context.applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * @return true if [sourcePackage] was in the foreground at any point in
     * [arrivalTs, now]. A small negative margin is applied to `arrivalTs` because
     * usage-event timestamps and notification timestamps come from slightly
     * different clocks and can be a few hundred ms apart.
     */
    fun wasSourceAppOpenedSince(
        sourcePackage: String,
        arrivalTs: Long,
        now: Long
    ): Boolean {
        val start = arrivalTs - CLOCK_SKEW_MARGIN_MS
        return try {
            val events = usageStatsManager.queryEvents(start, now)
            val event = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName == sourcePackage && isForegroundEvent(event.eventType)) {
                    // Only count foreground transitions that happened after arrival.
                    if (event.timeStamp >= arrivalTs - CLOCK_SKEW_MARGIN_MS) {
                        return true
                    }
                }
            }
            false
        } catch (_: SecurityException) {
            // Usage access not granted — we cannot prove a peek, so we do not invalidate here.
            // The ViewModel treats missing usage access as a separate, surfaced condition.
            false
        }
    }

    private fun isForegroundEvent(eventType: Int): Boolean =
        eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
            eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND

    private companion object {
        const val CLOCK_SKEW_MARGIN_MS = 1_000L
    }
}
