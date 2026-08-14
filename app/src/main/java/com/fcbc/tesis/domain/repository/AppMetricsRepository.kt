package com.fcbc.tesis.domain.repository

interface AppMetricsRepository {
    suspend fun getAppSize(): String
}
