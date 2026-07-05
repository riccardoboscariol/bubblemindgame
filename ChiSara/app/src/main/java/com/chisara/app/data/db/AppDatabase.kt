package com.chisara.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chisara.app.data.db.dao.ContactDao
import com.chisara.app.data.db.dao.GuessAttemptDao
import com.chisara.app.data.db.dao.NotificationEventDao
import com.chisara.app.data.db.entity.Contact
import com.chisara.app.data.db.entity.GuessAttempt
import com.chisara.app.data.db.entity.NotificationEvent

@Database(
    entities = [Contact::class, NotificationEvent::class, GuessAttempt::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun guessAttemptDao(): GuessAttemptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chisara.db"
                )
                    // Pre-release MVP: a schema change wipes local game history rather
                    // than shipping migrations. Replace with real migrations before release.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
