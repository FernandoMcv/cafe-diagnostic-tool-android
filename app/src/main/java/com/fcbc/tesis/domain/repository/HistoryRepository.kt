package com.fcbc.tesis.domain.repository

import com.fcbc.tesis.domain.model.HistoryItem
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryItem>>
    suspend fun addHistory(historyItem: HistoryItem): String
    suspend fun deleteHistory(historyItem: HistoryItem)
    suspend fun clearAll()
    suspend fun syncRemoteHistory(limit: Int = 100, offset: Int = 0)
}
