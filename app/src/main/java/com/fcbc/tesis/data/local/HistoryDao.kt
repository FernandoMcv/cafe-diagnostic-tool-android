package com.fcbc.tesis.data.local

import androidx.room.*
import com.fcbc.tesis.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface HistoryDao {

    @Query("SELECT * FROM history_table ORDER BY date DESC, time DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_table WHERE isSynced = 0")
    suspend fun getUnsyncedHistory(): List<HistoryEntity>

    @Query("SELECT * FROM history_table WHERE isSynced = 1")
    suspend fun getAllSynced(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity): Long

    @Update
    fun update(item: HistoryEntity): Int

    @Delete
    fun delete(item: HistoryEntity): Int

    @Query("DELETE FROM history_table")
    fun deleteAll(): Int
}
// Forzando invalidacion de cache de Kotlin/KSP
