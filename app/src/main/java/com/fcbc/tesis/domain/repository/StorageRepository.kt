package com.fcbc.tesis.domain.repository

import com.fcbc.tesis.domain.util.Resource

interface StorageRepository {
    /**
     * Sube una imagen a la nube y devuelve la URL pública.
     */
    suspend fun uploadImage(fileName: String, imageBytes: ByteArray): Resource<String>
}
