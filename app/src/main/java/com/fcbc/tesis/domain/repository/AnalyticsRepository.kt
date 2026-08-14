package com.fcbc.tesis.domain.repository

/**
 * Interfaz limpia (Clean Architecture) para el rastreo de métricas y errores.
 * No depende de ninguna librería de terceros (Firebase).
 */
interface AnalyticsRepository {
    
    /**
     * Registra un evento de analítica.
     * @param eventName El nombre del evento (ej. "scan_performed", "forum_post_created").
     * @param params Diccionario opcional de parámetros clave-valor para el evento.
     */
    fun trackEvent(eventName: String, params: Map<String, String> = emptyMap())

    /**
     * Registra una excepción no fatal para ser depurada en Crashlytics u otro servicio.
     */
    fun recordException(throwable: Throwable)
}
