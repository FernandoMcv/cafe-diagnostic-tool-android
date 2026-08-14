package com.fcbc.tesis.ui.tour

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import kotlinx.coroutines.delay

object TourManager {
    val currentStep = mutableStateOf(0)
    val targets = mutableStateMapOf<Int, Rect>()
    val MAX_STEP = 18

    fun start() { currentStep.value = 1 }
    fun next() { 
        if (currentStep.value < MAX_STEP) currentStep.value++ else skip()
    }
    fun previous() { currentStep.value = maxOf(1, currentStep.value - 1) }
    fun skip() { currentStep.value = 0 }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.tourTarget(step: Int): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val currentStep by TourManager.currentStep
    
    LaunchedEffect(currentStep) {
        if (currentStep == step) {
            // Pequeño delay para permitir que la pantalla termine de animarse o cargar
            delay(400)
            bringIntoViewRequester.bringIntoView()
        }
    }
    
    this.bringIntoViewRequester(bringIntoViewRequester)
        .onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            if (bounds.width > 0 && bounds.height > 0 && TourManager.targets[step] != bounds) {
                TourManager.targets[step] = bounds
            }
        }
}
