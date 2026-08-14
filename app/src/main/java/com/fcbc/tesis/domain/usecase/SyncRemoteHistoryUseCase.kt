package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.HistoryRepository

class SyncRemoteHistoryUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(limit: Int = 100, offset: Int = 0) = repository.syncRemoteHistory(limit, offset)
}
