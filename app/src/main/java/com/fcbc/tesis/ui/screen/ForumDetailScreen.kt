package com.fcbc.tesis.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fcbc.tesis.domain.model.Comment
import com.fcbc.tesis.domain.model.ForumPost
import com.fcbc.tesis.ui.viewmodel.ForumViewModel
import com.fcbc.tesis.ui.viewmodel.AuthViewModel
import com.fcbc.tesis.ui.viewmodel.AuthState
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground
import com.fcbc.tesis.ui.viewmodel.CommentNode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumDetailScreen(
    post: ForumPost,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onStartChatWithUser: (String, String) -> Unit,
    viewModel: ForumViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val organizedCommentsFlow = remember(post.id) { viewModel.getOrganizedComments(post.id) }
    val organizedComments by organizedCommentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val commentsFlow = remember(post.id) { viewModel.getComments(post.id) }
    val comments by commentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val currentUserId = (authState as? AuthState.Authenticated)?.userId
    
    var commentText by rememberSaveable { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }
    var showAuthRequiredDialog by remember { mutableStateOf(false) }
    
    var focusedCommentForThread by remember { mutableStateOf<CommentNode?>(null) }
    
    BackHandler(onBack = onBack)

    if (focusedCommentForThread != null) {
        ThreadDetailScreen(
            rootNode = focusedCommentForThread!!,
            onBack = { focusedCommentForThread = null },
            onReply = { parent, text ->
                if (authState is AuthState.Authenticated) {
                    viewModel.addComment(post.id, (authState as AuthState.Authenticated).userName, text, parent.id)
                } else {
                    showAuthRequiredDialog = true
                }
            }
        )
    } else {
        CoffeeThemedBackground {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text("Conversación", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                bottomBar = {
                    Column {
                        if (replyingTo != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Respondiendo a ${replyingTo!!.author}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            }
                        }
                        if (authState is AuthState.Authenticated) {
                            CommentInputField(
                                text = commentText,
                                onTextChange = { commentText = it },
                                placeholder = if (replyingTo != null) "Responder..." else "Asesorar técnicamente...",
                                onSend = {
                                    if (commentText.isNotBlank()) {
                                        val author = (authState as AuthState.Authenticated).userName
                                        viewModel.addComment(post.id, author, commentText, replyingTo?.id)
                                        commentText = ""
                                        replyingTo = null
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Respuesta enviada")
                                        }
                                    }
                                }
                            )
                        } else {
                            Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                                TextButton(
                                    onClick = { showAuthRequiredDialog = true },
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text("Inicia sesión para participar en el hilo", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    item {
                        PostDetailHeader(
                            post = post,
                            currentUserId = (authState as? AuthState.Authenticated)?.userId,
                            onStartChat = {
                                if (authState is AuthState.Authenticated) {
                                    if (post.userId != null) {
                                        // A user shouldn't chat with themselves
                                        if (post.userId != currentUserId) {
                                            onStartChatWithUser(post.userId, post.author)
                                        } else {
                                            coroutineScope.launch { snackbarHostState.showSnackbar("No puedes enviarte un mensaje a ti mismo") }
                                        }
                                    } else {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Usuario antiguo sin ID vinculado") }
                                    }
                                } else {
                                    showAuthRequiredDialog = true
                                }
                            },
                            onDelete = {
                                viewModel.deletePost(post.id)
                                onBack()
                            }
                        )
                    }
                    
                    item {
                        Text(
                            "Comentarios",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (comments.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No hay comentarios aún. ¡Sé el primero!", color = Color.Gray)
                            }
                        }
                    } else {
                        items(organizedComments) { commentNode ->
                            CommentHierarchy(
                                node = commentNode,
                                depth = 0,
                                onReply = { replyingTo = it },
                                onShowMore = { focusedCommentForThread = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAuthRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showAuthRequiredDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Sesión Requerida") },
            text = { Text("Debes iniciar sesión para participar en el hilo técnico y asesorar a otros productores.") },
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
fun CommentHierarchy(
    node: CommentNode,
    depth: Int,
    onReply: (Comment) -> Unit,
    onShowMore: (CommentNode) -> Unit
) {
    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        CommentItem(
            comment = node.comment,
            onReplyClick = { onReply(node.comment) }
        )
        
        if (depth < 3) {
            node.replies.forEach { child ->
                CommentHierarchy(child, depth + 1, onReply, onShowMore)
            }
        } else if (node.replies.isNotEmpty()) {
            TextButton(
                onClick = { onShowMore(node) },
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text("Ver resto de la conversación (${node.replies.size} respuestas) →", fontSize = 12.sp)
            }
        }
    }
}



@Composable
fun CommentItem(
    comment: Comment,
    onReplyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(comment.timestamp, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                IconButton(onClick = onReplyClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Responder", modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostDetailHeader(
    post: ForumPost,
    currentUserId: String? = null,
    onStartChat: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.LightGray).clickable { onStartChat() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Iniciar Chat")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    val formattedTime = try {
                        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.getDefault())
                        val parsedDate = parser.parse(post.timestamp)
                        val formatter = java.text.SimpleDateFormat("d/MM/yyyy h:mm a", java.util.Locale.getDefault())
                        formatter.format(parsedDate ?: java.util.Date())
                    } catch (e: Throwable) {
                        post.timestamp
                    }
                    Text("${post.cropContext} • $formattedTime", fontSize = 12.sp, color = Color.Gray)
                    if (!post.location.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(post.location, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
                if (post.userId != currentUserId) {
                    IconButton(onClick = onStartChat) {
                        Icon(Icons.Default.Email, contentDescription = "Mensaje Directo", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    TextButton(onClick = onDelete) {
                        Text("Eliminar", color = Color.Gray)
                    }
                }
            }
            
            if (post.diseaseTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    post.diseaseTags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(tag, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(post.content, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
            
            if (!post.imageUrl.isNullOrBlank()) {
                val images = post.imageUrl!!.split(",")
                Spacer(modifier = Modifier.height(16.dp))
                
                fun resolveImageModel(rawUrl: String): Any {
                    val trimmed = rawUrl.trim()
                    return when {
                        trimmed.isBlank() -> com.fcbc.tesis.R.drawable.logo
                        trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") || trimmed.startsWith("file://") -> trimmed
                        else -> java.io.File(trimmed)
                    }
                }

                if (images.size == 1) {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        AsyncImage(
                            model = resolveImageModel(images[0]),
                            contentDescription = null,
                            error = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                            placeholder = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.LightGray.copy(alpha = 0.3f)),
                            contentScale = ContentScale.Crop
                        )
                        if (post.aiConfidence != null) {
                            val percentage = (post.aiConfidence * 100).toInt()
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = com.fcbc.tesis.ui.theme.GreenPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "IA: $percentage%",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(images) { url ->
                            Box(modifier = Modifier.width(180.dp).height(180.dp)) {
                                AsyncImage(
                                    model = resolveImageModel(url),
                                    contentDescription = null,
                                    error = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                                    placeholder = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color.LightGray.copy(alpha = 0.3f)),
                                    contentScale = ContentScale.Crop
                                )
                                if (post.aiConfidence != null) {
                                    val percentage = (post.aiConfidence * 100).toInt()
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                                    ) {
                                        Text(
                                            text = "IA: $percentage%",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CommentInputField(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String = "Asesorar técnicamente...",
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text(placeholder) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
            }
        }
    }
}
