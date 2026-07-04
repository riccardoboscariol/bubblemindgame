package com.chisara.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The user's single guess for a [NotificationEvent].
 *
 * We keep [guessedContactName] as free text so a written-in name (not matching any
 * known contact) is still recorded. [scoreAwarded] is the log-score from
 * [com.chisara.app.scoring.Scoring]; 0 for a wrong (or invalidated) attempt.
 */
@Entity(
    tableName = "guess_attempts",
    foreignKeys = [
        ForeignKey(
            entity = NotificationEvent::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["eventId"], unique = true)]
)
data class GuessAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val guessedContactName: String,
    val correct: Boolean,
    val scoreAwarded: Int,
    val createdTs: Long
)
