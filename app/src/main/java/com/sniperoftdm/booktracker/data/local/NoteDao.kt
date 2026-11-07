package com.sniperoftdm.booktracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sniperoftdm.booktracker.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY page ASC, createdAt ASC")
    fun observeForBook(bookId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Note>>

    @Query("DELETE FROM notes WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Long)
}
