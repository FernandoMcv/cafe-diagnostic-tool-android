package com.fcbc.tesis.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fcbc.tesis.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) } // Estado para el Pop-up de ayuda

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraScreen", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // Overlay L-corners
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val boxWidth = canvasWidth * 0.70f
            val boxHeight = boxWidth * 1.5f // Hacerlo más alto que ancho (rectángulo)
            val left = (canvasWidth - boxWidth) / 2
            val top = (canvasHeight - boxHeight) * 0.4f // Ligeramente más arriba del centro
            val strokeWidth = 8.dp.toPx()
            val cornerLength = 40.dp.toPx()

            val color = Color.White
            // Top Left
            drawLine(color, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
            drawLine(color, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
            // Top Right
            drawLine(color, Offset(left + boxWidth, top), Offset(left + boxWidth - cornerLength, top), strokeWidth)
            drawLine(color, Offset(left + boxWidth, top), Offset(left + boxWidth, top + cornerLength), strokeWidth)
            // Bottom Left
            drawLine(color, Offset(left, top + boxHeight), Offset(left + cornerLength, top + boxHeight), strokeWidth)
            drawLine(color, Offset(left, top + boxHeight), Offset(left, top + boxHeight - cornerLength), strokeWidth)
            // Bottom Right
            drawLine(color, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth - cornerLength, top + boxHeight), strokeWidth)
            drawLine(color, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth, top + boxHeight - cornerLength), strokeWidth)
        }


        // Top Bar (Back on Left, Help on Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al Inicio", tint = Color.White)
            }

            IconButton(
                onClick = { showHelpDialog = true },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Ver instrucciones",
                    tint = Color.White
                )
            }
        }

        // Capture Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            if (isCapturing) {
                CircularProgressIndicator(color = Color.Green)
            } else {
                IconButton(
                    onClick = {
                        isCapturing = true
                        imageCapture?.let { capture ->
                            capture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        // CORRECCIÓN CRÍTICA: imageProxyToBitmap y cropBitmapToCenterSquare
                                        // hacen BitmapFactory.decodeByteArray y Bitmap.createBitmap.
                                        // Ejecutarlas en Main Thread bloquea la UI -> ANR.
                                        scope.launch(Dispatchers.IO) {
                                            val bitmap = imageProxyToBitmap(image)
                                            image.close()
                                            val cropped = cropBitmapToCenterSquare(bitmap)
                                            if (bitmap != cropped && !bitmap.isRecycled) {
                                                bitmap.recycle()
                                            }
                                            withContext(Dispatchers.Main) {
                                                isCapturing = false
                                                onImageCaptured(cropped)
                                            }
                                        }
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("CameraScreen", "Capture failed", exception)
                                        isCapturing = false
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White, CircleShape)
                        .padding(4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White, CircleShape))
                }
            }
        }


        // Pop-up Modal de Ayuda (Instrucciones Paso 1 y Paso 2)
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = {
                    Text(text = "Guía de Escaneo", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Para que la Inteligencia Artificial sea precisa, asegúrate de enfocar SOLO UNA HOJA que ocupe todo el recuadro, no la planta entera.",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // NOTA: Asegúrate de tener el archivo "camera_instruction_guide.png" en res/drawable/
                        Image(
                            painter = painterResource(id = R.drawable.camera_instruction_guide),
                            contentDescription = "Instrucciones de escaneo",
                            modifier = Modifier.fillMaxWidth().height(250.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showHelpDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Entendido", color = Color.White)
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    // Muestreo optimizado para evitar Bitmaps gigantes en RAM de camaras de 12MP/50MP
    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

    val maxDimension = 1024
    var sampleSize = 1
    while (boundsOptions.outWidth / sampleSize > maxDimension * 2 || boundsOptions.outHeight / sampleSize > maxDimension * 2) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) 
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    
    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    val rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
    
    if (originalBitmap != rotatedBitmap && !originalBitmap.isRecycled) {
        originalBitmap.recycle()
    }
    
    return rotatedBitmap
}

private fun cropBitmapToCenterSquare(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val minEdge = minOf(width, height)
    
    val cropSize = (minEdge * 0.75f).toInt().coerceAtLeast(1)
    val startX = ((width - cropSize) / 2).coerceAtLeast(0)
    val startY = ((height - cropSize) / 2).coerceAtLeast(0)
    
    val cropped = Bitmap.createBitmap(bitmap, startX, startY, cropSize, cropSize)
    
    // Escalar la imagen recortada a un tamaño máximo de 1024x1024 para la IA (optimización RAM Edge AI)
    val maxTarget = 1024
    if (cropped.width > maxTarget || cropped.height > maxTarget) {
        val scaled = Bitmap.createScaledBitmap(cropped, maxTarget, maxTarget, true)
        if (cropped != scaled && !cropped.isRecycled) {
            cropped.recycle()
        }
        return scaled
    }
    
    return cropped
}
