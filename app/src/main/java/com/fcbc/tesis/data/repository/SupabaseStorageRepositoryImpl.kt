package com.fcbc.tesis.data.repository

import com.fcbc.tesis.domain.repository.StorageRepository
import com.fcbc.tesis.domain.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseStorageRepositoryImpl(
    private val supabase: SupabaseClient
) : StorageRepository {

    private val BUCKET_NAME = "coffee-evidences"

    override suspend fun uploadImage(fileName: String, imageBytes: ByteArray): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val bucket = supabase.storage[BUCKET_NAME]
            
            // Subida básica para Supabase 3.0
            bucket.upload(
                path = fileName,
                data = imageBytes
            )

            // Obtener la URL pública
            val url = bucket.publicUrl(fileName)
            
            Resource.Success(url)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Error al subir imagen al servidor")
        }
    }
}
