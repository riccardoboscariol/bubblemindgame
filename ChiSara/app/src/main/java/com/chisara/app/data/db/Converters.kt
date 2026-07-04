package com.chisara.app.data.db

import androidx.room.TypeConverter
import com.chisara.app.data.db.entity.EventStatus

class Converters {
    @TypeConverter
    fun statusToString(status: EventStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): EventStatus = EventStatus.valueOf(value)
}
