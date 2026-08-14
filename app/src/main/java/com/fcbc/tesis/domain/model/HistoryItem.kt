package com.fcbc.tesis.domain.model

/**
 * Modelo de Dominio puro. Ya no tiene anotaciones de Room (@Entity).
 * Esto lo hace 100% independiente de la base de datos.
 */
data class HistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val diseaseName: String,
    val confidence: Float,
    val date: String,
    val time: String,
    val thumbnailPath: String,
    val location: String? = null
)
