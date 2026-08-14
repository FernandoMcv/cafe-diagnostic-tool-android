package com.fcbc.tesis.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcbc.tesis.domain.model.HistoryItem
import com.fcbc.tesis.domain.model.PredictionResult
import com.fcbc.tesis.domain.usecase.AnalyzeLeafUseCase
import com.fcbc.tesis.domain.usecase.SaveHistoryUseCase
import com.fcbc.tesis.domain.repository.AnalyticsRepository
import com.fcbc.tesis.domain.util.Resource
import com.fcbc.tesis.ui.util.toByteArray

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class HomeUiState(
    val predictionResult: PredictionResult? = null,
    val isAnalyzing: Boolean = false,
    val currentBitmap: Bitmap? = null,
    val errorMessage: String? = null,
    val remoteImageUrl: String? = null,
    val isSyncPending: Boolean = false
)

class HomeViewModel(
    private val analyzeLeafUseCase: AnalyzeLeafUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun analyzeImage(bitmap: Bitmap, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (bitmap.isRecycled) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isAnalyzing = false,
                            errorMessage = "La imagen no está disponible. Por favor seleccione o tome la foto nuevamente."
                        )
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isAnalyzing = true, errorMessage = null)
                }

                // Comprimimos el bitmap UNA SOLA VEZ a bytes en memoria
                val imageBytes = bitmap.toByteArray(quality = 85)

                // Guardamos esos mismos bytes al disco (sin volver a comprimir el bitmap)
                val localFilePath = try {
                    val file = java.io.File(context.filesDir, "leaf_${System.currentTimeMillis()}.jpg")
                    file.writeBytes(imageBytes)
                    file.absolutePath
                } catch (e: Exception) {
                    ""
                }

                val resource = analyzeLeafUseCase(imageBytes)

                withContext(Dispatchers.Main) {
                    when (resource) {
                        is Resource.Success -> {
                            val (prediction, remoteUrl) = resource.data!!
                            val finalPath = if (!remoteUrl.isNullOrBlank()) remoteUrl else localFilePath
                            
                            _uiState.value = _uiState.value.copy(
                                predictionResult = prediction,
                                isAnalyzing = false,
                                remoteImageUrl = finalPath,
                                isSyncPending = remoteUrl == null
                            )

                            // Siempre guardamos en el historial local con ruta de imagen válida
                            if (prediction.confidence > 0.65f) {
                                saveToHistory(prediction, finalPath)
                                
                                analyticsRepository.trackEvent("scan_performed", mapOf(
                                    "disease_detected" to prediction.label,
                                    "confidence_level" to prediction.confidence.toString()
                                ))
                            }
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isAnalyzing = false,
                                errorMessage = resource.message
                            )
                        }
                        is Resource.Loading -> {}
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        errorMessage = "Error en el análisis de imagen: ${t.localizedMessage ?: "Error desconocido"}"
                    )
                }
            }
        }
    }

    private suspend fun saveToHistory(prediction: PredictionResult, imageUrl: String) {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)

        val historyItem = HistoryItem(
            diseaseName = prediction.label,
            confidence = prediction.confidence,
            date = dateFormat.format(date),
            time = timeFormat.format(date),
            thumbnailPath = imageUrl
        )

        saveHistoryUseCase(historyItem)
    }

    fun clearPrediction() {
        val oldBitmap = _uiState.value.currentBitmap
        _uiState.value = HomeUiState()
        if (oldBitmap != null && !oldBitmap.isRecycled) {
            oldBitmap.recycle()
        }
    }

    fun setBitmap(bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        val oldBitmap = _uiState.value.currentBitmap
        if (oldBitmap != null && oldBitmap != bitmap && !oldBitmap.isRecycled) {
            oldBitmap.recycle()
        }
        _uiState.value = _uiState.value.copy(
            currentBitmap = bitmap,
            predictionResult = null,
            errorMessage = null,
            remoteImageUrl = null
        )
    }

    fun loadDemoImage(context: Context, resId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Prevenimos OOM cargando una versión escalada de la imagen (inSampleSize = 4 reduce la RAM 16x)
                val options = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, resId, options)
                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        setBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Limpieza al destruir el ViewModel (usuario cierra la app o navega permanentemente).
     * Los bitmaps tienen su pixel data en heap NATIVO (fuera del heap JVM), por lo que
     * el GC de Kotlin NO los libera eficientemente. recycle() es el estándar de industria
     * para evitar retención de memoria en dispositivos con poca RAM (ej. Xiaomi MIUI).
     */
    override fun onCleared() {
        super.onCleared()
        val bitmap = _uiState.value.currentBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
