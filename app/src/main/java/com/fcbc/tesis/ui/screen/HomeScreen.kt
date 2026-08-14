package com.fcbc.tesis.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcbc.tesis.domain.model.PredictionResult
import com.fcbc.tesis.ui.viewmodel.ForumViewModel
import com.fcbc.tesis.ui.viewmodel.HomeViewModel
import com.fcbc.tesis.ui.viewmodel.AuthViewModel
import com.fcbc.tesis.ui.viewmodel.AuthState
import com.fcbc.tesis.ui.viewmodel.HistoryViewModel
import java.io.InputStream
import com.fcbc.tesis.ui.theme.SuccessGreen
import com.fcbc.tesis.ui.theme.WarningOrange
import com.fcbc.tesis.ui.theme.ErrorRed
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground
import com.fcbc.tesis.ui.tour.TourManager
import com.fcbc.tesis.ui.tour.tourTarget
import com.fcbc.tesis.R

@Composable
fun CafeDetectionScreen(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit,
    isCameraOpen: Boolean,
    onCameraOpenChange: (Boolean) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
    forumViewModel: ForumViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    historyViewModel: HistoryViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val historyItems by historyViewModel.historyList.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Se necesita permiso de cámara para funcionar", Toast.LENGTH_LONG).show()
        } else {
            onCameraOpenChange(true)
        }
    }


    var isCreatingPost by remember { mutableStateOf(false) }
    var showAuthRequiredDialog by remember { mutableStateOf(false) }
    var showDemoGallery by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    // Estado para la imagen seleccionada de la galería pendiente de confirmación
    var pendingGalleryUri by remember { mutableStateOf<Uri?>(null) }

    val scope = rememberCoroutineScope()

    // Launcher del recortador manual con zoom/rotación
    val cropImage = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = uriToBitmap(context, uriContent)
                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                viewModel.setBitmap(bitmap)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error al cargar la imagen recortada.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            val error = result.error
            if (error != null) {
                Toast.makeText(context, "Recorte cancelado o fallido: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher del selector de galería
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // Guardamos el URI para abrir el diálogo de confirmación de edición
            pendingGalleryUri = uri
        }
    }

    if (isCameraOpen) {
        CameraScreen(
            onImageCaptured = { bitmap ->
                onCameraOpenChange(false)
                viewModel.setBitmap(bitmap)
            },
            onBack = { onCameraOpenChange(false) }
        )
        return
    }

    if (showDemoGallery) {
        DemoGalleryScreen(
            onDismiss = { showDemoGallery = false },
            onImageSelected = { resId ->
                showDemoGallery = false
                viewModel.loadDemoImage(context, resId)
            }
        )
        return
    }

    if (isCreatingPost && uiState.predictionResult != null) {
        PostCreationScreen(
            initialPrediction = uiState.predictionResult,
            initialImageUrl = uiState.remoteImageUrl,
            historyItems = historyItems,
            onBack = { isCreatingPost = false },
            onConfirm = { editedContent, finalImageUrl, tag ->
                val authorName = (authState as? AuthState.Authenticated)?.userName ?: "Productor"
                forumViewModel.addPost(
                    author = authorName,
                    context = "Detección desde App",
                    content = editedContent,
                    diseaseTags = listOf(tag),
                    imageUrl = finalImageUrl,
                    aiConfidence = uiState.predictionResult?.confidence
                )
                isCreatingPost = false
                Toast.makeText(context, "Publicado en la Comunidad", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        CoffeeThemedBackground { 
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { TourManager.start() }) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.HelpOutline, 
                            contentDescription = "Guía", 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = "Tome una foto de la hoja para analizar su salud.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                if (uiState.currentBitmap == null) {
                    SelectionArea(
                        onCameraClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onDemoClick = { showDemoGallery = true },
                        onShowHelp = { showHelpDialog = true }
                    )
                } else {
                    AnalysisArea(
                        bitmap = uiState.currentBitmap!!,
                        prediction = uiState.predictionResult,
                        isAnalyzing = uiState.isAnalyzing,
                        errorMessage = uiState.errorMessage,
                        isSyncPending = uiState.isSyncPending,
                        onRetake = {
                            viewModel.clearPrediction()
                        },
                        onAnalyze = {
                            uiState.currentBitmap?.let { bitmap ->
                                viewModel.analyzeImage(bitmap, context)
                            }
                        },
                        onShareToForum = {
                            if (authState is AuthState.Authenticated) {
                                isCreatingPost = true
                            } else {
                                showAuthRequiredDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAuthRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showAuthRequiredDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Sesión Requerida") },
            text = { Text("Debes iniciar sesión para publicar en la comunidad y compartir tus hallazgos con otros productores.") },
            confirmButton = {
                Button(onClick = { 
                    showAuthRequiredDialog = false
                    onNavigateToProfile()
                }) {
                    Text("Ir al Perfil / Iniciar Sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthRequiredDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("Guía para una buena foto", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Para un análisis preciso, asegúrate de que la hoja de café se vea clara, bien iluminada y centrada.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.camera_instruction_guide),
                        contentDescription = "Instrucciones de fotografía",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        )
    }
    
    if (pendingGalleryUri != null) {
        AlertDialog(
            onDismissRequest = { pendingGalleryUri = null },
            title = { Text("¿Deseas editar la foto?") },
            text = { Text("La imagen se procesará en su formato original. ¿Deseas recortarla o hacer zoom para enfocar mejor la hoja?") },
            confirmButton = {
                Button(
                    onClick = {
                        val uriToCrop = pendingGalleryUri
                        if (uriToCrop != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // Copia segura en caché local para evitar SecurityException en Android 11+
                                    val tempFile = java.io.File(context.cacheDir, "temp_gallery_input.jpg")
                                    context.contentResolver.openInputStream(uriToCrop)?.use { input ->
                                        tempFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    val safeUri = Uri.fromFile(tempFile)
                                    withContext(Dispatchers.Main) {
                                        val options = CropImageContractOptions(safeUri, CropImageOptions().apply {
                                            imageSourceIncludeGallery = false
                                            imageSourceIncludeCamera = false
                                            aspectRatioX = 1
                                            aspectRatioY = 1
                                            fixAspectRatio = true
                                            activityTitle = "Ajusta la hoja para el diagnóstico"
                                            guidelines = CropImageView.Guidelines.ON
                                        })
                                        cropImage.launch(options)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        pendingGalleryUri = null
                    }
                ) {
                    Text("Sí, editar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val uriToLoad = pendingGalleryUri
                        if (uriToLoad != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val bitmap = uriToBitmap(context, uriToLoad)
                                    if (bitmap != null) {
                                        withContext(Dispatchers.Main) {
                                            viewModel.setBitmap(bitmap)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        pendingGalleryUri = null
                    }
                ) {
                    Text("No, enviar así")
                }
            }
        )
    }
}

@Composable
fun SelectionArea(onCameraClick: () -> Unit, onGalleryClick: () -> Unit, onDemoClick: () -> Unit, onShowHelp: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Mensaje Agradable de Recordatorio
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp)
                .clickable { onShowHelp() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Recuerde tomar bien la foto", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 15.sp)
                    Text("Toque aquí para ver la guía rápida.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // El Botón Circular para la cámara
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            border = BorderStroke(4.dp, Color.White),
            modifier = Modifier
                .size(90.dp)
                .clickable { onCameraClick() }
                .tourTarget(2),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Tomar Foto", tint = Color.White, modifier = Modifier.size(45.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Tomar Foto", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)


        Spacer(modifier = Modifier.height(24.dp))
        Text("O busca en tus imágenes", fontWeight = FontWeight.ExtraBold, color = Color.Black, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onGalleryClick,
            modifier = Modifier.fillMaxWidth(0.85f).height(50.dp).tourTarget(3),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Icon(Icons.Outlined.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir Fotos", fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onDemoClick,
            modifier = Modifier.fillMaxWidth(0.85f).height(50.dp).tourTarget(4),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.DarkGray,
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Text("Imágenes Demo", fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("GUÍAS PARA UNA BUENA FOTO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            
            GuideCard(
                title = "Evite Sombras",
                subtitle = "Asegúrese de que la hoja esté bien iluminada y sin sombras de su mano o teléfono.",
                icon = Icons.Outlined.WbSunny,
                iconColor = com.fcbc.tesis.ui.theme.WarningOrange
            )
            Spacer(modifier = Modifier.height(12.dp))
            GuideCard(
                title = "Enfoque Claro",
                subtitle = "Sostenga el dispositivo firme hasta que la imagen se vea nítida en la pantalla.",
                icon = Icons.Outlined.CenterFocusWeak,
                iconColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun GuideCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(subtitle, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun AnalysisArea(
    bitmap: Bitmap,
    prediction: PredictionResult?,
    isAnalyzing: Boolean,
    errorMessage: String?,
    isSyncPending: Boolean,
    onRetake: () -> Unit,
    onAnalyze: () -> Unit,
    onShareToForum: (PredictionResult) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Card(
                elevation = CardDefaults.cardElevation(16.dp),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Hoja",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (!isAnalyzing) {
                IconButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (errorMessage != null) {
            Text(errorMessage, color = Color.Red, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            Button(onClick = onAnalyze) { Text("Reintentar") }
        } else if (prediction != null) {
            ResultCard(prediction)

            if (isSyncPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = WarningOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = WarningOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardado localmente. Sin respaldo en la nube.", color = WarningOrange, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onShareToForum(prediction) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPARTIR EN COMUNIDAD", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onRetake, 
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Analizar Otra Hoja")
            }

        } else if (isAnalyzing) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text("Analizando hoja...", modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.Medium)
        } else {
            Button(
                onClick = onAnalyze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .tourTarget(2),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("ANALIZAR AHORA", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        // 1. Medir dimensiones sin cargar la imagen completa a la RAM
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val maxDimension = 1024
        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > maxDimension * 2 || boundsOptions.outHeight / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val inputForRot = context.contentResolver.openInputStream(uri)
        val exif = inputForRot?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        inputForRot?.close()

        val rotatedBitmap = rotateBitmap(originalBitmap, orientation ?: ExifInterface.ORIENTATION_NORMAL)
        if (originalBitmap != rotatedBitmap && !originalBitmap.isRecycled) {
            originalBitmap.recycle()
        }

        val finalBitmap = rotatedBitmap ?: return null
        
        // Redimensionar respetando la relación de aspecto original (sin deformar ni aplastar) si excede 1024px
        val width = finalBitmap.width
        val height = finalBitmap.height
        if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val targetWidth: Int
            val targetHeight: Int
            if (width > height) {
                targetWidth = maxDimension
                targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                targetHeight = maxDimension
                targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
            }
            val scaled = Bitmap.createScaledBitmap(finalBitmap, targetWidth, targetHeight, true)
            if (scaled != finalBitmap && !finalBitmap.isRecycled) {
                finalBitmap.recycle()
            }
            return scaled
        }

        finalBitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun rotateBitmap(bitmap: Bitmap?, orientation: Int): Bitmap? {
    if (bitmap == null) return null
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@Composable
fun ResultCard(prediction: PredictionResult) {
    val (statusColor, statusText, mainText) = when {
        prediction.confidence > 0.65f -> Triple(
            SuccessGreen,
            "Diagnóstico Confiable",
            prediction.label
        )
        prediction.confidence > 0.50f -> Triple(
            WarningOrange,
            "Alerta de Calidad",
            "Escanee de nuevo o revise de cerca"
        )
        else -> Triple(
            ErrorRed,
            "Diagnóstico Fallido",
            "Imagen confusa, intente nuevamente"
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(2.dp, statusColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(modifier = Modifier.width(10.dp))
                Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = mainText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

        }
    }
}

@Composable
fun DemoGalleryScreen(
    onDismiss: () -> Unit,
    onImageSelected: (Int) -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    
    val demoImages = listOf(
        com.fcbc.tesis.R.drawable.demo_b_8,
        com.fcbc.tesis.R.drawable.demo_ca_38,
        com.fcbc.tesis.R.drawable.demo_fe_26,
        com.fcbc.tesis.R.drawable.demo_fe_46,
        com.fcbc.tesis.R.drawable.demo_mn_59,
        com.fcbc.tesis.R.drawable.demo_n_42
    )

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Imágenes de Prueba", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = com.fcbc.tesis.ui.theme.CreamBackground
    ) { paddingValues ->
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(demoImages) { resId ->
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = resId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onImageSelected(resId) }
                )
            }
        }
    }
}
