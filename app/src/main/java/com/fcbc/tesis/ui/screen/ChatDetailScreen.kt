package com.fcbc.tesis.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcbc.tesis.domain.model.ChatMessage
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground
import com.fcbc.tesis.ui.viewmodel.AuthState
import com.fcbc.tesis.ui.viewmodel.AuthViewModel
import com.fcbc.tesis.ui.viewmodel.ChatViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    roomId: String,
    participantName: String,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val messages by chatViewModel.currentMessages.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }

    val currentUserId = (authState as? AuthState.Authenticated)?.userId ?: ""

    LaunchedEffect(roomId) {
        chatViewModel.loadMessagesForRoom(roomId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = remember {
        listOf(
            "👋 ¡Hola! Me interesa tu consulta en el foro",
            "☕ ¿Qué variedad de café cultivas en tu finca?",
            "🌿 ¿Qué tratamiento aplicaste para la enfermedad?",
            "📍 ¿En qué zona o municipio estás ubicado?"
        )
    }

    CoffeeThemedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4CAF50),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(12.dp).align(Alignment.BottomEnd)
                                ) {}
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = participantName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Productor • En línea",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            chatViewModel.clearCurrentRoom()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Opciones */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                ) {
                    Column {
                        if (messages.isEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(quickPrompts) { prompt ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.clickable { inputText = prompt }
                                    ) {
                                        Text(
                                            text = prompt,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { /* Adjuntar imagen */ }) {
                                Icon(Icons.Outlined.Attachment, contentDescription = "Adjuntar", tint = Color.Gray)
                            }
                            
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Escribe un mensaje técnico...", color = Color.Gray, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f).heightIn(min = 46.dp, max = 120.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF5F3EF),
                                    unfocusedContainerColor = Color(0xFFF5F3EF),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            
                            Spacer(Modifier.width(8.dp))
                            
                            Surface(
                                shape = CircleShape,
                                color = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp),
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        chatViewModel.sendMessage(roomId, "", inputText)
                                        inputText = ""
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Enviar",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier.padding(24.dp).fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Charla Directa con $participantName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Coordina sugerencias de manejo agronómico, precios de mercado o intercambio de experiencias directamente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Surface(color = Color.White.copy(alpha = 0.85f), shape = RoundedCornerShape(12.dp)) {
                                Text(
                                    text = "Hoy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    items(messages) { message ->
                        ChatBubble(
                            message = message,
                            isMine = message.senderId == currentUserId
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isMine: Boolean) {
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary else Color.White
    val textColor = if (isMine) Color.White else Color.Black
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isMine) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) {
        try { timeFormat.format(Date(message.timestamp)) } catch (e: Exception) { "00:00" }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isMine) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Surface(
                shape = shape,
                color = bubbleColor,
                shadowElevation = 2.dp,
                modifier = if (isMine) Modifier.weight(1f, fill = false) else Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            color = if (isMine) Color.White.copy(alpha = 0.75f) else Color.Gray,
                            fontSize = 10.sp
                        )
                        if (isMine) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (message.isSent) Icons.Default.DoneAll else Icons.Default.AccessTime,
                                contentDescription = if (message.isSent) "Enviado" else "Pendiente de red",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            
            if (!isMine) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
