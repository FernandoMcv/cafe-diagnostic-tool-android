package com.fcbc.tesis.domain.util

/**
 * Clase sellada (Sealed Class) para manejar estados de respuesta de forma profesional.
 * Esto permite que la UI sepa exactamente si los datos son un éxito o un error.
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
