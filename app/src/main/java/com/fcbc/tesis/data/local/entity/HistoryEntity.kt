package com.fcbc.tesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de base de datos para el Historial de Análisis.
 */
@Entity(tableName = "history_table")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val diseaseName: String,
    val confidence: Float,
    val date: String,
    val time: String,
    val thumbnailPath: String,
    val location: String? = null,
    val isSynced: Boolean = false // Bandera vital para Offline-First
)
