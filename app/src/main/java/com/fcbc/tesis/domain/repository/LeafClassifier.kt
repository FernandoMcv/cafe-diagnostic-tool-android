package com.fcbc.tesis.domain.repository

import com.fcbc.tesis.domain.model.PredictionResult

interface LeafClassifier {
    suspend fun classify(imageBytes: ByteArray): PredictionResult
}

