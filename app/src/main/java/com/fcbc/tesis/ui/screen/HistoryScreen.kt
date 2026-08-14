package com.fcbc.tesis.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcbc.tesis.domain.model.HistoryItem
import com.fcbc.tesis.domain.model.PredictionResult
import com.fcbc.tesis.ui.viewmodel.HistoryViewModel
import com.fcbc.tesis.ui.theme.SuccessGreen
import com.fcbc.tesis.ui.theme.WarningOrange
import com.fcbc.tesis.ui.theme.ErrorRed
import com.fcbc.tesis.ui.viewmodel.AuthViewModel
import com.fcbc.tesis.ui.viewmodel.AuthState
import com.fcbc.tesis.ui.viewmodel.ForumViewModel
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground
import com.fcbc.tesis.ui.tour.tourTarget

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
    forumViewModel: ForumViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val historyItems by viewModel.historyList.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    var selectedItemToShare by remember { mutableStateOf<HistoryItem?>(null) }
    var selectedDetailItem by remember { mutableStateOf<HistoryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<HistoryItem?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAuthRequiredDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Primera llamada automática al entrar al apartado del historial
    LaunchedEffect(Unit) {
        viewModel.refreshHistory()
    }

    if (selectedItemToShare != null) {
        val item = selectedItemToShare!!
        PostCreationScreen(
            initialPrediction = PredictionResult(item.diseaseName, item.confidence),
            initialImageUrl = item.thumbnailPath,
            historyItems = historyItems,
            onBack = { selectedItemToShare = null },
            onConfirm = { editedContent, finalImageUrl, tag ->
                val authorName = (authState as? AuthState.Authenticated)?.userName ?: "Productor"
                forumViewModel.addPost(
                    author = authorName,
                    context = "Desde Historial",
                    content = editedContent,
                    diseaseTags = listOf(tag),
                    imageUrl = finalImageUrl,
                    aiConfidence = item.confidence
                )
                selectedItemToShare = null
            }
        )
    } else {
        CoffeeThemedBackground {
            Box(modifier = modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Historial de Análisis",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    isRefreshing = true
                                    android.widget.Toast.makeText(context, "Sincronizando historial...", android.widget.Toast.LENGTH_SHORT).show()
                                    viewModel.refreshHistory {
                                        isRefreshing = false
                                    }
                                }
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Actualizar Historial",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Text(
                        text = "Registro detallado de salud y nutrición de sus cultivos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                val categories = listOf("Todas") + historyItems.map { it.diseaseName }.distinct().sorted()
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).tourTarget(8),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray),
                            modifier = Modifier.clickable { selectedCategory = category }
                        ) {
                            Text(
                                text = if (category == "Todas") "Todos" else category,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else Color.DarkGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                PersistenceInfoCard(isLoggedIn = authState is AuthState.Authenticated)

                Spacer(modifier = Modifier.height(16.dp))

                val filteredItems = if (selectedCategory == "Todas") {
                    historyItems
                } else {
                    historyItems.filter { it.diseaseName == selectedCategory }
                }

                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (historyItems.isEmpty()) "Aún no has analizado ninguna hoja de café." else "No hay diagnósticos para esta categoría.", 
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.tourTarget(7),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            HistoryItemRow(
                                item = item,
                                onCardClick = { selectedDetailItem = item },
                                onDelete = { itemToDelete = item },
                                onShare = { 
                                    if (authState is AuthState.Authenticated) {
                                        selectedItemToShare = item 
                                    } else {
                                        showAuthRequiredDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { /* Navigate to Home or open camera */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Nueva Captura", modifier = Modifier.size(28.dp))
            }
            }
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
            title = { Text("Eliminar Análisis", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar este registro de diagnóstico?") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { viewModel.deleteItem(it) }
                        if (selectedDetailItem?.id == itemToDelete?.id) {
                            selectedDetailItem = null
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }


    if (selectedDetailItem != null) {
        HistoryDetailDialog(
            item = selectedDetailItem!!,
            onDismiss = { selectedDetailItem = null },
            onDelete = { itemToDelete = selectedDetailItem },
            onShare = {
                val item = selectedDetailItem!!
                selectedDetailItem = null
                if (authState is AuthState.Authenticated) {
                    selectedItemToShare = item
                } else {
                    showAuthRequiredDialog = true
                }
            }
        )
    }

    if (showAuthRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showAuthRequiredDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Sesión Requerida") },
            text = { Text("Debes iniciar sesión para compartir tus diagnósticos con la comunidad.") },
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
}

@Composable
fun PersistenceInfoCard(isLoggedIn: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLoggedIn) SuccessGreen.copy(alpha = 0.1f) else WarningOrange.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLoggedIn) Icons.Default.CloudDone else Icons.Default.Info,
                contentDescription = null,
                tint = if (isLoggedIn) SuccessGreen else WarningOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (isLoggedIn) {
                    "Tus datos están protegidos y respaldados en la nube."
                } else {
                    "Tus análisis se guardan solo en este celular. Inicia sesión para respaldarlos en la nube."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLoggedIn) Color(0xFF1B5E20) else Color(0xFFE65100),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun HistoryItemRow(
    item: HistoryItem,
    onCardClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val isSana = item.diseaseName.lowercase().contains("sana") || item.diseaseName.lowercase().contains("saludable")
    val isDeficiencia = item.diseaseName.lowercase().contains("falta") || item.diseaseName.lowercase().contains("deficiencia")
    
    val (statusColor, statusText) = when {
        isSana -> SuccessGreen to "SALUDABLE"
        isDeficiencia -> WarningOrange to "DEFICIENCIA"
        else -> ErrorRed to "ALERTA PLAGA"
    }

    val imageModel: Any = remember(item.thumbnailPath) {
        val path = item.thumbnailPath
        when {
            path.isBlank() -> com.fcbc.tesis.R.drawable.logo
            path.startsWith("http://") || path.startsWith("https://") || path.startsWith("content://") || path.startsWith("file://") -> path
            else -> java.io.File(path)
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                placeholder = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${(item.confidence * 100).toInt()}% Confianza",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.diseaseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.date,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    if (!item.location.isNullOrBlank()) {
                        Text("  •  ", color = Color.Gray, fontSize = 12.sp)
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.location,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onShare,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Compartir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryDetailDialog(
    item: HistoryItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val isSana = item.diseaseName.lowercase().contains("sana") || item.diseaseName.lowercase().contains("saludable")
    val isDeficiencia = item.diseaseName.lowercase().contains("falta") || item.diseaseName.lowercase().contains("deficiencia")
    
    val (statusColor, statusText) = when {
        isSana -> SuccessGreen to "SALUDABLE"
        isDeficiencia -> WarningOrange to "DEFICIENCIA NUTRICIONAL"
        else -> ErrorRed to "ALERTA PLAGA / ENFERMEDAD"
    }

    val imageModel: Any = remember(item.thumbnailPath) {
        val path = item.thumbnailPath
        when {
            path.isBlank() -> com.fcbc.tesis.R.drawable.logo
            path.startsWith("http://") || path.startsWith("https://") || path.startsWith("content://") || path.startsWith("file://") -> path
            else -> java.io.File(path)
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AsyncImage(
                    model = imageModel,
                    contentDescription = item.diseaseName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = item.diseaseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Precisión IA: ${(item.confidence * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("  •  ${item.date} ${item.time}", fontSize = 13.sp, color = Color.DarkGray)
                }

                if (!item.location.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = item.location, fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Recomendación Técnica Agronómica:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSana) {
                        "La muestra no presenta síntomas visibles de deficiencias o plagas. Mantenga el plan de fertilización foliar de rutina y el manejo de sombras en la parcela."
                    } else {
                        "Se sugiere evaluar el pH del suelo y aplicar corrección foliar organomineral dirigida según los síntomas detectados. Consulte con un agrónomo para dosificación exacta."
                    },
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Borrar", fontWeight = FontWeight.Bold, color = ErrorRed, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onShare,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Compartir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
