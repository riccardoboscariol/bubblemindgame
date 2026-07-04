package com.chisara.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chisara.app.data.db.entity.EventStatus
import com.chisara.app.data.db.entity.NotificationEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationEventDao {

    @Insert
    suspend fun insert(event: NotificationEvent): Long

    @Update
    suspend fun update(event: NotificationEvent)

    @Query("SELECT * FROM notification_events WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): NotificationEvent?

    @Query("SELECT * FROM notification_events WHERE status = :status ORDER BY arrivalTs ASC")
    fun observeByStatus(status: EventStatus = EventStatus.PENDING): Flow<List<NotificationEvent>>

    @Query("SELECT * FROM notification_events WHERE status = :status ORDER BY arrivalTs ASC")
    suspend fun listByStatus(status: EventStatus): List<NotificationEvent>

    @Query("SELECT COUNT(*) FROM notification_events WHERE status = :status")
    fun countByStatus(status: EventStatus = EventStatus.PENDING): Flow<Int>
}
