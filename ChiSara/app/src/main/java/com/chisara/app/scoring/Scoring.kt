package com.chisara.app.scoring

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure, Android-free scoring logic so it can be unit-tested in isolation.
 *
 * Scoring is information-based (self-information / log-score): guessing a rare
 * sender is worth more than guessing a chatterbox. If a contact accounts for a
 * fraction `p` of all messages received in that app, a correct guess is worth
 *
 *     score = round(K * -log2(p))
 *
 * clamped to [0, maxScore]. A wrong guess is worth [Config.wrongAnswerPenalty]
 * (0 by default; may be negative if the user opts into penalties).
 */
object Scoring {

    data class Config(
        /** Scale constant K applied to the bits of self-information. */
        val scale: Double = 10.0,
        /** Upper bound on a single correct answer's score. */
        val maxScore: Int = 100,
        /** Points for a wrong answer. 0 by default; set negative to punish. */
        val wrongAnswerPenalty: Int = 0
    )

    private val LOG2 = ln(2.0)

    private fun log2(x: Double): Double = ln(x) / LOG2

    /**
     * Empirical prior p(contact) = messages from this contact / total messages in the app.
     *
     * Uses add-one (Laplace) smoothing so a brand-new contact (count 0, or a total of
     * 0) still gets a finite, meaningful probability instead of p=0 → infinite score.
     *
     * @param contactMessageCount historical messages from the guessed/real contact
     * @param totalMessagesInApp   total messages ever received in that app
     * @param distinctContacts     number of distinct contacts seen in that app (for smoothing)
     */
    fun priorProbability(
        contactMessageCount: Long,
        totalMessagesInApp: Long,
        distinctContacts: Int
    ): Double {
        val smoothingContacts = max(distinctContacts, 1)
        val numerator = contactMessageCount.toDouble() + 1.0
        val denominator = totalMessagesInApp.toDouble() + smoothingContacts.toDouble()
        // Guard against pathological inputs.
        val p = numerator / denominator
        return min(max(p, MIN_PROBABILITY), 1.0)
    }

    /** Self-information (surprise) in bits for a contact with prior [p]. */
    fun selfInformationBits(p: Double): Double = -log2(p)

    /**
     * Score for a correct guess of a contact whose empirical prior is [p].
     * Rarer contact (small p) → more bits → higher score, capped at [Config.maxScore].
     */
    fun scoreForCorrect(p: Double, config: Config = Config()): Int {
        val bits = selfInformationBits(p)
        val raw = (config.scale * bits).roundToInt()
        return raw.coerceIn(0, config.maxScore)
    }

    /**
     * Full scoring entry point.
     *
     * @param correct did the user name the right sender?
     * @param contactMessageCount historical count for the REAL sender
     * @param totalMessagesInApp   total messages in that app
     * @param distinctContacts     distinct contacts seen in that app
     */
    fun score(
        correct: Boolean,
        contactMessageCount: Long,
        totalMessagesInApp: Long,
        distinctContacts: Int,
        config: Config = Config()
    ): Int {
        if (!correct) return config.wrongAnswerPenalty
        val p = priorProbability(contactMessageCount, totalMessagesInApp, distinctContacts)
        return scoreForCorrect(p, config)
    }

    private const val MIN_PROBABILITY = 1e-6
}
