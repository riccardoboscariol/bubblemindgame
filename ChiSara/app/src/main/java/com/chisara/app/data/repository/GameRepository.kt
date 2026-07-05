package com.chisara.app.data.repository

import android.content.Context
import com.chisara.app.data.db.AppDatabase
import com.chisara.app.data.db.dao.ContactAccuracy
import com.chisara.app.data.db.entity.Contact
import com.chisara.app.data.db.entity.EventStatus
import com.chisara.app.data.db.entity.GuessAttempt
import com.chisara.app.data.db.entity.NotificationEvent
import com.chisara.app.data.settings.SettingsRepository
import com.chisara.app.notification.BlindNotifier
import com.chisara.app.scoring.Scoring
import com.chisara.app.usage.UsageChecker
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth used by both the [com.chisara.app.notification.ChiSaraNotificationListener]
 * (write side: interception) and the ViewModel (read side + guessing).
 */
class GameRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val contacts = db.contactDao()
    private val events = db.notificationEventDao()
    private val guesses = db.guessAttemptDao()
    private val blindNotifier = BlindNotifier(appContext)
    private val usageChecker = UsageChecker(appContext)
    private val settingsRepository = SettingsRepository(appContext)

    // ---- Read side (for the UI) -------------------------------------------------

    fun observePendingEvents(): Flow<List<NotificationEvent>> =
        events.observeByStatus(EventStatus.PENDING)

    fun observePendingCount(): Flow<Int> = events.countByStatus(EventStatus.PENDING)

    fun observeTotalScore(): Flow<Int> = guesses.observeTotalScore()
    fun observeTotalAttempts(): Flow<Int> = guesses.observeTotalAttempts()
    fun observeCorrectCount(): Flow<Int> = guesses.observeCorrectCount()
    fun observeContactAccuracy(minAttempts: Int = 1): Flow<List<ContactAccuracy>> =
        guesses.observeContactAccuracy(minAttempts)
    fun observeAttempts(): Flow<List<GuessAttempt>> = guesses.observeAll()

    suspend fun getEvent(eventId: Long): NotificationEvent? = events.findById(eventId)

    /** Contacts to offer in the guess picker for the event's app, most-recent first. */
    suspend fun recentContactsFor(sourcePackage: String, limit: Int = 30): List<Contact> =
        contacts.recentContactsForPackage(sourcePackage, limit)

    // ---- Write side (interception) ---------------------------------------------

    /**
     * Records one intercepted, non-previewable notification: upserts the sender,
     * bumps its historical count (keeps the prior fresh), creates a PENDING event,
     * and posts the blind replacement notification.
     *
     * @return the new event id, or null if it could not be recorded.
     */
    suspend fun recordInterceptedNotification(
        senderName: String,
        sourcePackage: String,
        arrivalTs: Long
    ): Long {
        val contactId = upsertContactAndIncrement(senderName, sourcePackage, arrivalTs)

        // Insert the event first (without notification id), then post the blind
        // notification (whose id is derived from the event id), then patch it in.
        val event = NotificationEvent(
            realContactId = contactId,
            realSenderName = senderName,
            sourcePackage = sourcePackage,
            arrivalTs = arrivalTs,
            status = EventStatus.PENDING,
            replacementNotificationId = 0
        )
        val eventId = events.insert(event)

        val notificationId = blindNotifier.postBlind(eventId, sourcePackage, arrivalTs)
        events.update(event.copy(id = eventId, replacementNotificationId = notificationId))
        return eventId
    }

    private suspend fun upsertContactAndIncrement(
        name: String,
        pkg: String,
        ts: Long
    ): Long {
        val existing = contacts.findByNameAndPackage(name, pkg)
        val id = existing?.id ?: run {
            val inserted = contacts.insert(Contact(displayName = name, sourcePackage = pkg))
            // insert() with IGNORE returns -1 on conflict; re-read to be safe.
            if (inserted != -1L) inserted
            else contacts.findByNameAndPackage(name, pkg)!!.id
        }
        contacts.incrementMessageCount(id, ts)
        return id
    }

    // ---- Guessing ---------------------------------------------------------------

    sealed interface GuessOutcome {
        /** The event no longer exists or was already answered. */
        data object Unavailable : GuessOutcome

        /** The user peeked at the source app before guessing; no score awarded. */
        data class Invalidated(val realSenderName: String, val reason: String) : GuessOutcome

        /** A counted guess: reveals the real sender, correctness and score. */
        data class Revealed(
            val realSenderName: String,
            val guessedName: String,
            val correct: Boolean,
            val score: Int,
            val priorProbability: Double,
            val infoBits: Double
        ) : GuessOutcome
    }

    /**
     * Submits the user's guess for [eventId]. Enforces the anti-peek rule via
     * [UsageChecker] before scoring, records the attempt, and dismisses the blind
     * notification.
     */
    suspend fun submitGuess(
        eventId: Long,
        guessedName: String,
        now: Long,
        config: Scoring.Config? = null
    ): GuessOutcome {
        val event = events.findById(eventId) ?: return GuessOutcome.Unavailable
        if (event.status != EventStatus.PENDING) return GuessOutcome.Unavailable

        // Fall back to the user's configured scoring (wrong-answer penalty).
        val scoringConfig = config ?: settingsRepository.current().toScoringConfig()

        blindNotifier.cancelBlind(event.replacementNotificationId)

        // Anti-peek: did the user open the source app between arrival and now?
        val peeked = usageChecker.wasSourceAppOpenedSince(
            sourcePackage = event.sourcePackage,
            arrivalTs = event.arrivalTs,
            now = now
        )
        if (peeked) {
            val reason = "Hai aperto ${appNameFor(event.sourcePackage)} prima di rispondere"
            events.update(
                event.copy(
                    status = EventStatus.INVALIDATED,
                    responseTs = now,
                    invalidationReason = reason
                )
            )
            return GuessOutcome.Invalidated(event.realSenderName, reason)
        }

        val correct = normalize(guessedName) == normalize(event.realSenderName)

        val messageCount = contacts.findById(event.realContactId)?.historicalMessageCount ?: 1
        val totalInApp = contacts.totalMessagesForPackage(event.sourcePackage)
        val distinct = contacts.distinctContactsForPackage(event.sourcePackage)
        val prior = Scoring.priorProbability(messageCount, totalInApp, distinct)
        val bits = Scoring.selfInformationBits(prior)
        val score = Scoring.score(correct, messageCount, totalInApp, distinct, scoringConfig)

        events.update(event.copy(status = EventStatus.VALID, responseTs = now))
        guesses.insert(
            GuessAttempt(
                eventId = eventId,
                guessedContactName = guessedName,
                correct = correct,
                scoreAwarded = score,
                createdTs = now
            )
        )

        return GuessOutcome.Revealed(
            realSenderName = event.realSenderName,
            guessedName = guessedName,
            correct = correct,
            score = score,
            priorProbability = prior,
            infoBits = bits
        )
    }

    private fun appNameFor(pkg: String) =
        com.chisara.app.notification.NotificationConfig.displayName(pkg)

    private fun normalize(s: String) = s.trim().lowercase()
}
