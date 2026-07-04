package com.chisara.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Status of a single intercepted notification within the guessing game.
 */
enum class EventStatus {
    /** Intercepted and replaced with a blind notification; waiting for the user to guess. */
    PENDING,

    /** The user guessed without peeking; the attempt counted. */
    VALID,

    /** Discarded: the user opened the source app (or otherwise saw the message) before guessing,
     *  or the notification was previewable / a group message / removed before the guess. */
    INVALIDATED
}

/**
 * One intercepted notification that entered the game.
 *
 * [realContactId] is resolved eagerly when the notification is intercepted so the
 * true sender is known before the user answers. [realSenderName] keeps a denormalised
 * copy so reveal still works if the contact row is ever cleaned up.
 */
@Entity(tableName = "notification_events")
data class NotificationEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val realContactId: Long,
    val realSenderName: String,
    val sourcePackage: String,
    val arrivalTs: Long,
    val responseTs: Long? = null,
    val status: EventStatus = EventStatus.PENDING,
    /** Reason a PENDING event was invalidated, for display; null while valid/pending. */
    val invalidationReason: String? = null,
    /** Id of the replacement notification we posted, so we can dismiss it once answered. */
    val replacementNotificationId: Int
)
