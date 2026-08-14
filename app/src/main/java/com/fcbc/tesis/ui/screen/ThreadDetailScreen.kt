package com.fcbc.tesis.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fcbc.tesis.domain.model.Comment
import com.fcbc.tesis.ui.viewmodel.CommentNode
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailScreen(
    rootNode: CommentNode,
    onBack: () -> Unit,
    onReply: (Comment, String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf(rootNode.comment) }

    CoffeeThemedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Hilo de conversación", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                CommentInputField(
                    text = commentText,
                    onTextChange = { commentText = it },
                    placeholder = "Respondiendo a ${replyingTo.author}...",
                    onSend = {
                        if (commentText.isNotBlank()) {
                            onReply(replyingTo, commentText)
                            commentText = ""
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Conversación iniciada por:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        // El comentario raíz no lleva sangría aquí
                        CommentItem(comment = rootNode.comment, onReplyClick = { replyingTo = rootNode.comment })
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))
                }

                items(rootNode.replies) { replyNode ->
                        // Empezamos profundidad 0 para que las respuestas del raíz tengan sangría 1
                        CommentHierarchy(
                            node = replyNode,
                            depth = 0,
                            onReply = { replyingTo = it },
                            onShowMore = { /* En esta pantalla mostramos todo sin límite o con límite mayor */ }
                        )
                }
            }
        }
    }
}
