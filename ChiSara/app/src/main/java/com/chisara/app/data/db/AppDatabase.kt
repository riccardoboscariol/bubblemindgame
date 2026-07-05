package com.chisara.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

        /** v1 → v2: adds the nullable group name to notification_events. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notification_events ADD COLUMN groupName TEXT")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chisara.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
