package com.chisara.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chisara.app.data.db.entity.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts WHERE displayName = :name AND sourcePackage = :pkg LIMIT 1")
    suspend fun findByNameAndPackage(name: String, pkg: String): Contact?

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Contact?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: Contact): Long

    /**
     * Records that we saw one more message from this contact. Keeps the empirical
     * prior fresh whether or not the user ends up guessing this event.
     */
    @Query(
        """
        UPDATE contacts
        SET historicalMessageCount = historicalMessageCount + 1,
            lastContactTs = :ts
        WHERE id = :id
        """
    )
    suspend fun incrementMessageCount(id: Long, ts: Long)

    /** Total messages ever received across all contacts of a given app. Denominator of the prior. */
    @Query("SELECT COALESCE(SUM(historicalMessageCount), 0) FROM contacts WHERE sourcePackage = :pkg")
    suspend fun totalMessagesForPackage(pkg: String): Long

    /** Number of distinct contacts seen in an app; used to smooth the prior. */
    @Query("SELECT COUNT(*) FROM contacts WHERE sourcePackage = :pkg")
    suspend fun distinctContactsForPackage(pkg: String): Int

    /** Recent contacts for the guess picker, most-recently-active first. */
    @Query(
        """
        SELECT * FROM contacts
        WHERE sourcePackage = :pkg
        ORDER BY lastContactTs DESC
        LIMIT :limit
        """
    )
    suspend fun recentContactsForPackage(pkg: String, limit: Int = 30): List<Contact>

    @Query("SELECT * FROM contacts ORDER BY lastContactTs DESC")
    fun observeAll(): Flow<List<Contact>>
}
