package com.fcbc.tesis.domain.util

/**
 * Utilidades puras de Kotlin para el dominio.
 */

// Genera un nombre de archivo único para evidencias
fun generateEvidenceName(): String = "leaf_${System.currentTimeMillis()}.jpg"
