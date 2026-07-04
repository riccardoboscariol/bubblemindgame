package com.chisara.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chisara.app.data.db.entity.GuessAttempt
import kotlinx.coroutines.flow.Flow

/** Aggregated stats row used by the statistics screen. */
data class ContactAccuracy(
    val displayName: String,
    val sourcePackage: String,
    val attempts: Int,
    val correct: Int
)

@Dao
interface GuessAttemptDao {

    @Insert
    suspend fun insert(attempt: GuessAttempt): Long

    @Query("SELECT * FROM guess_attempts WHERE eventId = :eventId LIMIT 1")
    suspend fun findByEvent(eventId: Long): GuessAttempt?

    @Query("SELECT COALESCE(SUM(scoreAwarded), 0) FROM guess_attempts")
    fun observeTotalScore(): Flow<Int>

    @Query("SELECT COUNT(*) FROM guess_attempts")
    fun observeTotalAttempts(): Flow<Int>

    @Query("SELECT COUNT(*) FROM guess_attempts WHERE correct = 1")
    fun observeCorrectCount(): Flow<Int>

    @Query("SELECT * FROM guess_attempts ORDER BY createdTs ASC")
    fun observeAll(): Flow<List<GuessAttempt>>

    /**
     * Per-contact accuracy: joins each attempt to the real sender of its event so
     * the stats screen can show who the user is best / worst at recognising.
     */
    @Query(
        """
        SELECT c.displayName AS displayName,
               c.sourcePackage AS sourcePackage,
               COUNT(*) AS attempts,
               SUM(CASE WHEN g.correct THEN 1 ELSE 0 END) AS correct
        FROM guess_attempts g
        JOIN notification_events e ON e.id = g.eventId
        JOIN contacts c ON c.id = e.realContactId
        GROUP BY c.id
        HAVING attempts >= :minAttempts
        ORDER BY (CAST(correct AS REAL) / attempts) DESC
        """
    )
    fun observeContactAccuracy(minAttempts: Int = 1): Flow<List<ContactAccuracy>>
}
