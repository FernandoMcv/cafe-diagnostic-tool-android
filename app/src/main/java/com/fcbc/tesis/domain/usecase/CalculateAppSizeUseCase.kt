package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.AppMetricsRepository

class CalculateAppSizeUseCase(private val appMetricsRepository: AppMetricsRepository) {
    suspend operator fun invoke(): String {
        return appMetricsRepository.getAppSize()
    }
}
