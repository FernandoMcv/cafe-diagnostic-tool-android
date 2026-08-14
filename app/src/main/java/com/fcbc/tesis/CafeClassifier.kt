package com.fcbc.tesis

import com.fcbc.tesis.domain.model.PredictionResult
import kotlin.math.exp
import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CafeClassifier(context: Context) {
    private var module: Module? = null
    var errorDeCarga: String? = null

    companion object {
        @Volatile
        private var INSTANCE: CafeClassifier? = null

        fun getInstance(context: Context): CafeClassifier {
            return INSTANCE ?: synchronized(this) {
                val instance = CafeClassifier(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    init {
        try {
            val modelPath = assetFilePath(context, "model_cafe.ptl")
            module = LiteModuleLoader.load(modelPath)
        } catch (e: Throwable) {
            e.printStackTrace()
            errorDeCarga = "Error al cargar modelo: ${e.message}. Verifica que el modelo .ptl esté exportado para PyTorch Mobile."
        }
    }

    fun classify(bitmap: Bitmap): PredictionResult {
        val loadError = errorDeCarga
        if (loadError != null) {
            android.util.Log.e("CafeClassifier", loadError)
            return PredictionResult("Error: Modelo no disponible", 0f)
        }
        if (module == null) return PredictionResult("Error: Módulo no cargado", 0f)

        return try {
            val bitmapReescalado = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmapReescalado,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
            )
            // Memory Profiling: Liberar el bitmap reescalado si se creo una copia nueva
            if (bitmapReescalado != bitmap && !bitmapReescalado.isRecycled) {
                bitmapReescalado.recycle()
            }
            
            val outputTensor = try {
                module!!.forward(IValue.from(inputTensor)).toTensor()
            } catch (e: Exception) {
                android.util.Log.e("CafeClassifier", "Error en inferencia: ${e.message}")
                return PredictionResult("Error: Formato de modelo incompatible", 0f)
            }
            val rawScores = outputTensor.dataAsFloatArray

            val probabilities = softmax(rawScores)

            val maxScoreIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
            val maxScore = if (maxScoreIdx != -1) probabilities[maxScoreIdx] else 0f

            // CAMBIO 3: Aquí van las enfermedades/deficiencias del café.
            // Modifícalas si tu modelo fue entrenado con otras clases.
            val labels = listOf(
                "Deficiencia Boro (B)",        // 0 -> boron-B
                "Deficiencia Calcio (Ca)",     // 1 -> calcium-Ca
                "Deficiencia Hierro (Fe)",     // 2 -> iron-Fe
                "Deficiencia Magnesio (Mg)",   // 3 -> magnesium-Mg
                "Deficiencia Manganeso (Mn)",  // 4 -> manganese-Mn
                "Otras Deficiencias",          // 5 -> more-deficiencies
                "Deficiencia Nitrógeno (N)",   // 6 -> nitrogen-N
                "Deficiencia Fósforo (P)",     // 7 -> phosphorus-P
                "Deficiencia Potasio (K)"      // 8 -> potasium-K
            )

            val labelFinal = if (maxScoreIdx >= 0 && maxScoreIdx < labels.size) {
                labels[maxScoreIdx]
            } else {
                "Desconocido"
            }

            PredictionResult(labelFinal, maxScore)

        } catch (e: Throwable) {
            PredictionResult("Error: ${e.message}", 0f)
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        var maxLogit = Float.NEGATIVE_INFINITY
        for (logit in logits) {
            if (logit > maxLogit) {
                maxLogit = logit
            }
        }

        val expScores = logits.map { exp(it - maxLogit) }
        val sumExp = expScores.sum()
        return expScores.map { (it / sumExp).toFloat() }.toFloatArray()
    }

    @Throws(IOException::class)
    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) outputStream.write(buffer, 0, read)
                outputStream.flush()
            }
        }
        return file.absolutePath
    }
}