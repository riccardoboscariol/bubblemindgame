package com.chisara.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A person who has sent the user a message through one of the tracked apps.
 *
 * Identity is (displayName + sourcePackage): the same human on WhatsApp and on
 * Telegram counts as two contacts, because the empirical prior we score against
 * is per-app. [historicalMessageCount] is the running total of notifications we
 * have ever seen from this contact and is the numerator of the p(contact) prior.
 */
@Entity(
    tableName = "contacts",
    indices = [Index(value = ["displayName", "sourcePackage"], unique = true)]
)
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val sourcePackage: String,
    val historicalMessageCount: Long = 0,
    val lastContactTs: Long = 0
)
