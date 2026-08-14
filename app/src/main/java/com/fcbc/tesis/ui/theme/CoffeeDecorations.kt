package com.fcbc.tesis.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Componente de diseño Senior para dar identidad visual cafetalera.
 */
@Composable
fun CoffeeThemedBackground(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CreamBackground,
                        Color(0xFFDFE6E0) // Un verde salvia suave, con más presencia
                    )
                )
            )
    ) {
        // Dibujamos las hojas decorativas en el fondo
        LeafDecorations()
        
        // El contenido principal de la pantalla
        content()
    }
}

@Composable
private fun LeafDecorations() {
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.08f)) {
        // Hoja en la esquina superior izquierda
        drawLeaf(
            offset = Offset(-50f, -50f),
            size = Size(300f, 400f),
            rotation = 45f,
            color = SuccessGreen
        )
        
        // Hoja en la esquina inferior derecha
        drawLeaf(
            offset = Offset(size.width - 250f, size.height - 350f),
            size = Size(350f, 450f),
            rotation = -135f,
            color = GreenPrimary
        )

        // Hoja pequeña decorativa a la mitad
        drawLeaf(
            offset = Offset(size.width - 100f, size.height * 0.4f),
            size = Size(150f, 200f),
            rotation = -30f,
            color = SuccessGreen
        )
    }
}

private fun DrawScope.drawLeaf(
    offset: Offset,
    size: Size,
    rotation: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(0f, 0f)
        // Curva superior de la hoja
        quadraticTo(size.width / 2, -size.height / 4, size.width, 0f)
        // Curva inferior de la hoja
        quadraticTo(size.width / 2, size.height / 1.5f, 0f, 0f)
        close()
    }
    
    // Aplicamos rotación y posición
    drawContext.canvas.save()
    drawContext.canvas.translate(offset.x, offset.y)
    drawContext.canvas.rotate(rotation)
    drawPath(path = path, color = color)
    
    // Dibujamos el nervio central de la hoja
    drawLine(
        color = color.copy(alpha = 0.3f),
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 3f
    )
    drawContext.canvas.restore()
}
