package com.sniperoftdm.booktracker.data.local

import androidx.room.TypeConverter
import com.sniperoftdm.booktracker.data.model.BookStatus

class Converters {

    @TypeConverter
    fun fromBookStatus(status: BookStatus): String = status.name

    @TypeConverter
    fun toBookStatus(value: String): BookStatus = BookStatus.valueOf(value)
}
