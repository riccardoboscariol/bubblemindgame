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
    val wrongAnswerPenalty: Int = 5
) {
    /** Turns these settings into the pure [Scoring.Config] used by the scorer. */
    fun toScoringConfig(): Scoring.Config =
        Scoring.Config(
            wrongAnswerPenalty = if (penaltyEnabled) -wrongAnswerPenalty else 0
        )
}
