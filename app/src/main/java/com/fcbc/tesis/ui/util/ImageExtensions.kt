package com.fcbc.tesis.ui.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

// Convierte un Bitmap a ByteArray comprimido en una sola línea
fun Bitmap.toByteArray(quality: Int = 80): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return stream.toByteArray()
}
