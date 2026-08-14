package com.fcbc.tesis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcbc.tesis.domain.model.HistoryItem
import com.fcbc.tesis.domain.usecase.ClearHistoryUseCase
import com.fcbc.tesis.domain.usecase.DeleteHistoryItemUseCase
import com.fcbc.tesis.domain.usecase.GetHistoryUseCase
import com.fcbc.tesis.domain.usecase.CalculateAppSizeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


import com.fcbc.tesis.domain.model.PredictionResult
import com.fcbc.tesis.domain.usecase.AnalyzeLeafUseCase
import com.fcbc.tesis.domain.usecase.SaveHistoryUseCase
import com.fcbc.tesis.domain.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryViewModel(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val deleteHistoryItemUseCase: DeleteHistoryItemUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val calculateAppSizeUseCase: CalculateAppSizeUseCase,
    private val syncRemoteHistoryUseCase: com.fcbc.tesis.domain.usecase.SyncRemoteHistoryUseCase,
    private val analyzeLeafUseCase: AnalyzeLeafUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase
) : ViewModel() {

    val historyList: StateFlow<List<HistoryItem>> = getHistoryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _appSize = MutableStateFlow("Calculando...")
    val appSize: StateFlow<String> = _appSize.asStateFlow()

    private val _reanalyzingId = MutableStateFlow<String?>(null)
    val reanalyzingId: StateFlow<String?> = _reanalyzingId.asStateFlow()

    init {
        updateAppSize()
        viewModelScope.launch {
            try {
                syncRemoteHistoryUseCase()
            } catch (e: Exception) {
                // Silencioso: fallo de red al arrancar no debe cerrar la app.
                // El historial local de Room sigue disponible.
                e.printStackTrace()
            }
        }
    }

    fun refreshHistory(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                syncRemoteHistoryUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete?.invoke()
            }
        }
    }

    fun updateAppSize() {
        viewModelScope.launch {
            _appSize.value = calculateAppSizeUseCase()
        }
    }

    fun deleteItem(item: HistoryItem) {
        viewModelScope.launch {
            deleteHistoryItemUseCase(item)
            updateAppSize()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            clearHistoryUseCase()
            updateAppSize()
        }
    }

    fun reanalyzeItem(item: HistoryItem, onResult: (PredictionResult?) -> Unit) {
        viewModelScope.launch {
            _reanalyzingId.value = item.id
            try {
                val imageBytes = withContext(Dispatchers.IO) {
                    val path = item.thumbnailPath
                    when {
                        path.isBlank() -> null
                        path.startsWith("http://") || path.startsWith("https://") -> {
                            try { java.net.URL(path).readBytes() } catch (e: Exception) { null }
                        }
                        else -> {
                            val file = java.io.File(path)
                            if (file.exists()) file.readBytes() else null
                        }
                    }
                }

                if (imageBytes != null) {
                    val resource = analyzeLeafUseCase(imageBytes)
                    if (resource is Resource.Success) {
                        val newPrediction = resource.data!!.first
                        val updatedItem = item.copy(
                            diseaseName = newPrediction.label,
                            confidence = newPrediction.confidence
                        )
                        saveHistoryUseCase(updatedItem)
                        onResult(newPrediction)
                    } else {
                        onResult(null)
                    }
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            } finally {
                _reanalyzingId.value = null
            }
        }
    }
}
