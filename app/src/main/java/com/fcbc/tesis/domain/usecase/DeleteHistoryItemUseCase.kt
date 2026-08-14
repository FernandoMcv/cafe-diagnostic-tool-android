package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.HistoryItem
import com.fcbc.tesis.domain.repository.HistoryRepository

class DeleteHistoryItemUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(item: HistoryItem) {
        historyRepository.deleteHistory(item)
    }
}
