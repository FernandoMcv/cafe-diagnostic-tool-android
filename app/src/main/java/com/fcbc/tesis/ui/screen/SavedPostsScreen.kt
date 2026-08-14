package com.fcbc.tesis.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground
import com.fcbc.tesis.ui.viewmodel.UserInteractionsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    onBack: () -> Unit,
    viewModel: UserInteractionsViewModel = koinViewModel()
) {
    val savedPosts by viewModel.savedPosts.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts()
    }

    CoffeeThemedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Posts Guardados", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            if (savedPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aún no has guardado ningún post.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(paddingValues).fillMaxSize()
                ) {
                    items(savedPosts) { post ->
                        PostCard(
                            post = post,
                            onClick = { /* Could open detail if wanted */ },
                            onLikeClick = { },
                            onAuthorClick = { _, _ -> },
                            onSaveClick = { },
                            onFollowClick = { },
                            onChatClick = { _, _ -> }
                        )
                    }
                }
            }
        }
    }
}
