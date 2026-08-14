package com.fcbc.tesis.data.classifier

import android.content.Context
    import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.fcbc.tesis.CafeClassifier
import com.fcbc.tesis.domain.model.PredictionResult
import com.fcbc.tesis.domain.repository.LeafClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PytorchLeafClassifier(
    private val context: Context
) : LeafClassifier {
    
    private val classifier by lazy { CafeClassifier.getInstance(context) }

    override suspend fun classify(imageBytes: ByteArray): PredictionResult = withContext(Dispatchers.IO) {
        var decodedBitmap: Bitmap? = null
        return@withContext try {
            decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return@withContext PredictionResult("Error: Formato de imagen no soportado", 0f)
            // CafeClassifier crea internamente un Bitmap 224x224 y lo recicla él mismo.
            // decodedBitmap se recicla en el bloque finally, DESPUÉS de que classify() termina.
            classifier.classify(decodedBitmap)
        } catch (e: Throwable) {
            e.printStackTrace()
            PredictionResult("Error en análisis IA: ${e.message}", 0f)
        } finally {
            // finally garantiza que el recycle ocurre SIEMPRE y DESPUÉS de la inferencia completa.
            decodedBitmap?.let { if (!it.isRecycled) it.recycle() }
        }
    }
}
