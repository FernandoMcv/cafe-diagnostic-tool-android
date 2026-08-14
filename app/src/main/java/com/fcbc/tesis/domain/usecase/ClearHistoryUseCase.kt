package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.HistoryRepository

class ClearHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke() {
        historyRepository.clearAll()
    }
}
