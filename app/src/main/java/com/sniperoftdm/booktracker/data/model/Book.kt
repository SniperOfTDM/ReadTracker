package com.sniperoftdm.booktracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val author: String = "",
    val totalPages: Int,
    val startPage: Int = 0,
    val status: BookStatus = BookStatus.READING,
    val goalDateEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isbn: String? = null,
    val openLibraryKey: String? = null,
    val coverPath: String? = null
)
