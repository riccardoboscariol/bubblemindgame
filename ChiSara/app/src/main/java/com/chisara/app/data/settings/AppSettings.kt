package com.chisara.app.data.settings

import com.chisara.app.notification.NotificationConfig
import com.chisara.app.scoring.Scoring

/**
 * User-configurable settings. Persisted with DataStore and read by both the
 * notification listener (tracked packages) and the scoring path (penalty).
 */
data class AppSettings(
    /** Packages whose notifications enter the game. */
    val trackedPackages: Set<String> = NotificationConfig.trackedPackages,
    /** When true, a wrong guess costs [wrongAnswerPenalty] points instead of 0. */
    val penaltyEnabled: Boolean = false,
    /** Points applied on a wrong guess when [penaltyEnabled]; stored as a positive magnitude. */
    val wrongAnswerPenalty: Int = 5,
    /** When true, group messages also enter the game (guess who wrote in the group). */
    val includeGroups: Boolean = false,
    /**
     * When true, notifications whose body text is readable (previews on) still enter
     * the game — we hide the text ourselves in the blind notification. This is what
     * makes the game trigger on all three apps with their default settings. The trade-off
     * is that the original notification may flash as a heads-up before we replace it.
     */
    val playWithPreviewOn: Boolean = true,
    /**
     * Research/strict mode. When true, a message only becomes a guessing round if its
     * notification would NOT be shown as a heads-up banner (channel importance below
     * HIGH) — i.e. the user could not have seen the sender pop up. Guarantees the game
     * only counts messages that were genuinely hidden. Requires the user to set the
     * messaging app's notifications to "silent / no pop-up".
     */
    val onlyWhenNotVisible: Boolean = true
) {
    /** Turns these settings into the pure [Scoring.Config] used by the scorer. */
    fun toScoringConfig(): Scoring.Config =
        Scoring.Config(
            wrongAnswerPenalty = if (penaltyEnabled) -wrongAnswerPenalty else 0
        )
}
