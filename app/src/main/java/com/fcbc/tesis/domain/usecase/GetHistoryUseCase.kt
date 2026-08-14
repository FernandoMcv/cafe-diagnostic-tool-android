package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.HistoryItem
import com.fcbc.tesis.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class GetHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(): Flow<List<HistoryItem>> {
        return historyRepository.getAllHistory()
    }
}
