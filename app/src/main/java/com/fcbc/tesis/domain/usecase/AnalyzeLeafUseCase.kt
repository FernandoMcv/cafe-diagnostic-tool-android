package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.PredictionResult
import com.fcbc.tesis.domain.repository.LeafClassifier
import com.fcbc.tesis.domain.repository.StorageRepository
import com.fcbc.tesis.domain.util.Resource
import com.fcbc.tesis.domain.util.generateEvidenceName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalyzeLeafUseCase(
    private val leafClassifier: LeafClassifier,
    private val storageRepository: StorageRepository
) {
    /**
     * Versión Blindada (Senior): IA local primero, subida opcional.
     */
    suspend operator fun invoke(imageBytes: ByteArray): Resource<Pair<PredictionResult, String?>> = withContext(Dispatchers.IO) {
        try {
            // 1. Ejecutamos la IA inmediatamente (Local) usando los bytes
            val prediction = leafClassifier.classify(imageBytes)
            
            if (prediction.label.startsWith("Error")) {
                return@withContext Resource.Error(prediction.label)
            }

            // 2. Intentamos la subida, pero no bloqueamos el flujo principal
            val fileName = generateEvidenceName()

            val uploadResult = try {
                storageRepository.uploadImage(fileName, imageBytes)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Sin conexión")
            }

            val remoteUrl = if (uploadResult is Resource.Success) uploadResult.data else null

            // Retornamos la predicción local SIEMPRE, incluso si remoteUrl es null.
            Resource.Success(Pair(prediction, remoteUrl))

        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error crítico en el proceso")
        }
    }
}
