package com.fcbc.tesis.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground
import androidx.compose.foundation.BorderStroke
import com.fcbc.tesis.domain.model.HistoryItem
import com.fcbc.tesis.domain.model.PredictionResult
import com.fcbc.tesis.domain.util.TechConstants

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostCreationScreen(
    initialPrediction: PredictionResult?,
    initialImageUrl: String?,
    historyItems: List<HistoryItem>,
    onBack: () -> Unit,
    onConfirm: (String, String?, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { 
        mutableStateOf(
            if (initialPrediction != null) "He detectado ${initialPrediction.label} con la IA. ¿Qué tratamiento recomiendan?" 
            else ""
        ) 
    }
    
    val selectedImages = remember { mutableStateListOf<String>().apply { 
        initialImageUrl?.let { add(it) } 
    } }
    
    val selectedTags = remember { mutableStateListOf<String>().apply {
        initialPrediction?.label?.let { add(it) }
    } }
    
    var showHistoryPicker by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    CoffeeThemedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(48.dp)) // StatusBar padding roughly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.DarkGray)
                }
                Text("Nueva Publicación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = com.fcbc.tesis.ui.theme.GreenPrimary)
                Button(
                    onClick = { 
                        val finalImages = selectedImages.joinToString(",")
                        val finalTags = selectedTags.joinToString(", ")
                        val fullContent = if (title.isNotBlank()) "$title\n$content" else content
                        onConfirm(fullContent, if (finalImages.isEmpty()) null else finalImages, finalTags) 
                    },
                    enabled = content.isNotBlank() || title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = com.fcbc.tesis.ui.theme.GreenPrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Publicar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            
            Spacer(Modifier.height(24.dp))

            Text("Título de la publicación", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. Problemas con roya en lote 3") },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = com.fcbc.tesis.ui.theme.GreenPrimary
                )
            )

            Spacer(Modifier.height(24.dp))

            Text("Evidencias Fotográficas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(selectedImages) { url ->
                    val imgModel: Any = remember(url) {
                        val trimmed = url.trim()
                        when {
                            trimmed.isBlank() -> com.fcbc.tesis.R.drawable.logo
                            trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") || trimmed.startsWith("file://") -> trimmed
                            else -> java.io.File(trimmed)
                        }
                    }
                    Box(modifier = Modifier.size(160.dp)) {
                        AsyncImage(
                            model = imgModel,
                            contentDescription = null,
                            error = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                            placeholder = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImages.remove(url) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape).size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                
                item {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Transparent)
                            .clickable { showHistoryPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        // Dashed border effect using an outlined card or custom drawing
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, com.fcbc.tesis.ui.theme.GreenPrimary.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = com.fcbc.tesis.ui.theme.GreenPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text("Añadir", color = com.fcbc.tesis.ui.theme.GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Location mockup section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6F4)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = com.fcbc.tesis.ui.theme.GreenPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ubicación del Reporte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = com.fcbc.tesis.ui.theme.GreenPrimary.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, com.fcbc.tesis.ui.theme.GreenPrimary)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = com.fcbc.tesis.ui.theme.GreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Usar mi ubicación (Cajamarca)", color = com.fcbc.tesis.ui.theme.GreenPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Añadir ubicación personalizada", color = Color.DarkGray)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Categorías Técnicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TechConstants.forumCategories.filter { it != "Todas" }.forEach { category ->
                    val isSelected = selectedTags.contains(category)
                    Surface(
                        color = if (isSelected) com.fcbc.tesis.ui.theme.GreenPrimary else Color.White,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.LightGray),
                        modifier = Modifier.clickable {
                            if (isSelected) selectedTags.remove(category) else selectedTags.add(category)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = "#$category", 
                                color = if (isSelected) Color.White else Color.DarkGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.Gray), // Dashed effect ideally
                    modifier = Modifier.clickable { }
                ) {
                    Text("+ Nueva", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.DarkGray)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Consulta para la Comunidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                placeholder = { Text("Describe los síntomas observados...") },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = com.fcbc.tesis.ui.theme.GreenPrimary
                )
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showHistoryPicker) {
        HistoryPickerDialog(
            items = historyItems,
            onDismiss = { showHistoryPicker = false },
            onSelect = { item ->
                if (!selectedImages.contains(item.thumbnailPath)) {
                    selectedImages.add(item.thumbnailPath)
                }
                if (!selectedTags.contains(item.diseaseName)) {
                    selectedTags.add(item.diseaseName)
                }
                showHistoryPicker = false
            }
        )
    }
}

@Composable
fun HistoryPickerDialog(
    items: List<HistoryItem>,
    onDismiss: () -> Unit,
    onSelect: (HistoryItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Análisis", fontWeight = FontWeight.Bold) },
        text = {
            if (items.isEmpty()) {
                Text("No tienes diagnósticos guardados.", modifier = Modifier.padding(vertical = 24.dp))
            } else {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(items) { item ->
                        Card(
                            onClick = { onSelect(item) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pickerImgModel: Any = remember(item.thumbnailPath) {
                                    val trimmed = item.thumbnailPath.trim()
                                    when {
                                        trimmed.isBlank() -> com.fcbc.tesis.R.drawable.logo
                                        trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") || trimmed.startsWith("file://") -> trimmed
                                        else -> java.io.File(trimmed)
                                    }
                                }
                                AsyncImage(
                                    model = pickerImgModel,
                                    contentDescription = null,
                                    error = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                                    placeholder = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(item.diseaseName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${item.date} • ${item.time}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
