package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.HistoryItem
import com.fcbc.tesis.domain.repository.HistoryRepository

class SaveHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(historyItem: HistoryItem): String {
        return historyRepository.addHistory(historyItem)
    }
}
