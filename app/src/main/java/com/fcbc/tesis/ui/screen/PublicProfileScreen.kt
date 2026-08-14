package com.fcbc.tesis.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground
import com.fcbc.tesis.ui.viewmodel.PublicProfileViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    userName: String,
    onBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    viewModel: PublicProfileViewModel = koinViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
    val userPosts by viewModel.userPosts.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    CoffeeThemedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(profile?.name ?: userName, fontWeight = FontWeight.Bold) },
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
            LazyColumn(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!profile?.avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = profile?.avatarUrl,
                                    contentDescription = "Avatar de ${profile?.name ?: userName}",
                                    modifier = Modifier.size(80.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = profile?.name ?: userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            if (!profile?.location.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(profile!!.location!!, color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${userPosts.size} publicaciones", color = Color.Gray, fontSize = 14.sp)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.toggleFollow(userId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) Color.LightGray else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isFollowing) Color.DarkGray else Color.White
                                    )
                                ) {
                                    Icon(
                                        if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isFollowing) "Siguiendo" else "Seguir",
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onNavigateToChat(userId, userName) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Mensaje",
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Publicaciones de ${profile?.name ?: userName}", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
                }

                if (userPosts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Este usuario aún no ha publicado nada.", color = Color.Gray)
                        }
                    }
                } else {
                    items(userPosts) { post ->
                        PostCard(
                            post = post,
                            onClick = { /* View Detail */ },
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
