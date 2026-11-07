package com.sniperoftdm.booktracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sniperoftdm.booktracker.data.model.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): Session?

    @Query("SELECT * FROM sessions WHERE bookId = :bookId ORDER BY timestamp ASC, id ASC")
    fun observeForBook(bookId: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE bookId = :bookId ORDER BY timestamp ASC, id ASC")
    suspend fun getForBook(bookId: Long): List<Session>

    @Query("SELECT * FROM sessions ORDER BY timestamp ASC, id ASC")
    fun observeAll(): Flow<List<Session>>

    @Query(
        "SELECT * FROM sessions WHERE bookId = :bookId " +
            "ORDER BY timestamp DESC, id DESC LIMIT 1"
    )
    suspend fun getLastForBook(bookId: Long): Session?

    @Query(
        "SELECT * FROM sessions WHERE bookId = :bookId " +
            "AND (timestamp < :timestamp OR (timestamp = :timestamp AND id < :id)) " +
            "ORDER BY timestamp DESC, id DESC LIMIT 1"
    )
    suspend fun getPreviousSession(bookId: Long, timestamp: Long, id: Long): Session?

    @Query(
        "SELECT * FROM sessions WHERE bookId = :bookId " +
            "AND (timestamp > :timestamp OR (timestamp = :timestamp AND id > :id)) " +
            "ORDER BY timestamp ASC, id ASC LIMIT 1"
    )
    suspend fun getNextSession(bookId: Long, timestamp: Long, id: Long): Session?
}
