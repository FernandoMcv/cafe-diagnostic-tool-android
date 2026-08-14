package com.fcbc.tesis.data.mapper

import com.fcbc.tesis.data.local.entity.HistoryEntity
import com.fcbc.tesis.domain.model.HistoryItem
import kotlinx.serialization.Serializable

@Serializable
data class HistoryDto(
    val id: Long? = null,
    val disease_name: String,
    val confidence: Float,
    val date: String,
    val time: String,
    val thumbnail_path: String,
    val user_id: String? = null
)

// Mapeos usando sintaxis compacta de Kotlin
fun HistoryEntity.toDomain() = HistoryItem(id, diseaseName, confidence, date, time, thumbnailPath, location)
fun HistoryItem.toEntity() = HistoryEntity(id, diseaseName, confidence, date, time, thumbnailPath, location, false)

fun HistoryDto.toEntity() = HistoryEntity(
    id = (id ?: System.currentTimeMillis()).toString(),
    diseaseName = disease_name,
    confidence = confidence,
    date = date,
    time = time,
    thumbnailPath = thumbnail_path,
    location = null,
    isSynced = true
)

fun HistoryEntity.toDto(userId: String? = null) = HistoryDto(
    id = null, // Supabase autogenera la clave int8 (1, 2, 3...)
    disease_name = diseaseName,
    confidence = confidence,
    date = date,
    time = time,
    thumbnail_path = thumbnailPath,
    user_id = userId
)
