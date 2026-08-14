package com.fcbc.tesis.data.repository

import com.fcbc.tesis.data.local.HistoryDao
import com.fcbc.tesis.data.mapper.HistoryDto
import com.fcbc.tesis.data.mapper.toDomain
import com.fcbc.tesis.data.mapper.toDto
import com.fcbc.tesis.data.mapper.toEntity
import com.fcbc.tesis.domain.model.HistoryItem
import com.fcbc.tesis.domain.repository.HistoryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val supabase: SupabaseClient
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<HistoryItem>> {
        return historyDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addHistory(historyItem: HistoryItem): String = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull()
        var location: String? = null
        if (user != null) {
            try {
                val profileDto = supabase.postgrest["profiles"]
                    .select { filter { eq("id", user.id) } }
                    .decodeSingleOrNull<ProfileDto>()
                location = profileDto?.location
            } catch (e: Exception) {}
        }
        
        var isSyncedFlag = false

        try {
            if (user != null) {
                val dto = historyItem.toEntity().copy(location = location).toDto(user.id)
                supabase.postgrest["history"].insert(dto)
                isSyncedFlag = true // Subida exitosa a la nube
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Guardado local definitivo (Offline-First)
        val historyWithLocation = historyItem.toEntity().copy(location = location, isSynced = isSyncedFlag)
        historyDao.insert(historyWithLocation)
        
        historyItem.id
    }

    override suspend fun deleteHistory(historyItem: HistoryItem) = withContext(Dispatchers.IO) {
        historyDao.delete(historyItem.toEntity())
        
        try {
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                val targetId: Any = historyItem.id.toLongOrNull() ?: historyItem.id
                supabase.postgrest["history"].delete {
                    filter {
                        eq("id", targetId)
                        eq("user_id", user.id)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        historyDao.deleteAll()
        Unit
    }

    override suspend fun syncRemoteHistory(limit: Int, offset: Int) {
        withContext(Dispatchers.IO) {
            try {
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    val remoteHistory = supabase.postgrest["history"]
                        .select {
                            filter { eq("user_id", user.id) }
                            range(offset.toLong(), (offset + limit - 1).toLong())
                        }
                        .decodeList<HistoryDto>()

                    val remoteEntities = remoteHistory.map { it.toEntity() }
                    val remoteIds = remoteEntities.map { it.id }.toSet()

                    remoteEntities.forEach { entity ->
                        historyDao.insert(entity)
                    }

                    if (offset == 0) {
                        val localSynced = historyDao.getAllSynced()
                        localSynced.forEach { local ->
                            if (local.id !in remoteIds) {
                                historyDao.delete(local)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
