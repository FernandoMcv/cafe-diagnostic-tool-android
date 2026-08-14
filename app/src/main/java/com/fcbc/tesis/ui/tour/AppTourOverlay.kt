package com.fcbc.tesis.ui.tour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class TourTooltip(val title: String, val description: String)

val tourTooltips = mapOf(
    2 to TourTooltip("Uso de Cámara", "Captura una foto de tu planta para realizar un análisis de salud en tiempo real."),
    3 to TourTooltip("Uso de Galería", "También puedes seleccionar una foto de tu galería para analizarla."),
    4 to TourTooltip("Imágenes de Prueba", "Usa estas imágenes predefinidas para probar la herramienta de diagnóstico de forma rápida."),
    5 to TourTooltip("Pantalla Inicio", "Volver al inicio para realizar un nuevo análisis."),
    6 to TourTooltip("Pantalla Historial", "Aquí se guardan automáticamente todos tus diagnósticos."),
    7 to TourTooltip("Lista de Resultados", "Tus diagnósticos recientes. Tócalos para ver los detalles y consejos."),
    8 to TourTooltip("Filtros Inteligentes", "Filtra rápidamente tu historial por enfermedad o salud de la planta."),
    9 to TourTooltip("Comunidad", "Accede al foro donde los productores comparten información vital."),
    10 to TourTooltip("Buscador de Casos", "Busca enfermedades, consejos o problemas específicos subidos por otros usuarios."),
    11 to TourTooltip("Interacción en Posts", "Puedes comentar, guardar, seguir a otros y, si es tuyo, eliminar el post."),
    12 to TourTooltip("Chats Privados", "Inicia conversaciones uno a uno con expertos o colegas."),
    13 to TourTooltip("Tus Conversaciones", "Bandeja de entrada segura y cifrada para mensajería directa."),
    14 to TourTooltip("Perfil de Usuario", "Gestiona tu identidad y el contenido que has guardado."),
    15 to TourTooltip("Tarjeta de Identidad", "Tu información básica que será visible en la comunidad."),
    16 to TourTooltip("Tus Favoritos", "Acceso rápido a todos los tips y posts que has guardado del foro."),
    17 to TourTooltip("Red de Contactos", "Personas que sigues para mantenerte actualizado con sus posts."),
    18 to TourTooltip("Ajustes Legales", "Gestiona tu privacidad, términos y condiciones del sistema.")
)

@Composable
fun AppTourOverlay() {
    val step = TourManager.currentStep.value
    if (step == 0) return

    if (step == 1) {
        AlertDialog(
            onDismissRequest = { TourManager.skip() },
            title = { Text("Bienvenido al Tour", fontWeight = FontWeight.Bold) },
            text = { Text("Está por iniciar una guía interactiva para conocer todas las funciones principales de la app. ¿Desea continuar?") },
            confirmButton = {
                Button(onClick = { TourManager.next() }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { TourManager.skip() }) { Text("Saltar", color = Color.Gray) }
            }
        )
        return
    }

    val targetRect = TourManager.targets[step] ?: Rect.Zero
    val tooltip = tourTooltips[step]

    BackHandler(enabled = step > 0) {
        TourManager.skip()
    }

    // Dialog o Box a nivel de root. Usamos Box para el canvas completo.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.99f) // Necesario para BlendMode.Clear
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Bloqueamos toques al fondo, pero dejamos que los botones del Tour reaccionen primero
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.forEach { 
                            if (!it.isConsumed) {
                                it.consume() 
                            }
                        }
                    }
                }
            }
    ) {
        // Lienzo para oscurecer y recortar
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.75f))
            if (targetRect != Rect.Zero) {
                // Perforar el rectángulo objetivo con bordes redondeados
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(targetRect.left, targetRect.top),
                    size = Size(targetRect.width, targetRect.height),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }

        // Botón Saltar en esquina superior derecha
        IconButton(
            onClick = { TourManager.skip() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Saltar", tint = Color.White)
        }

        // Tooltip
        if (tooltip != null) {
            val alignTop = targetRect.bottom > 1200f // Si el elemento está muy abajo, el tooltip va arriba
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = if (alignTop) 80.dp else 40.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(if (alignTop) Alignment.TopCenter else Alignment.BottomCenter)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Text(tooltip.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tooltip.description, fontSize = 14.sp, color = Color.DarkGray)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$step / ${TourManager.MAX_STEP}",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            TextButton(onClick = { TourManager.previous() }) {
                                Text("Atrás")
                            }
                            Button(onClick = { TourManager.next() }) {
                                Text(if (step == TourManager.MAX_STEP) "Terminar" else "Siguiente")
                            }
                        }
                    }
                }
            }
        }
    }
}
