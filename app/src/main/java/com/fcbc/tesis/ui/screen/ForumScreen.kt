package com.fcbc.tesis.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.paging.LoadState
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcbc.tesis.domain.model.ForumPost
import com.fcbc.tesis.domain.model.PredictionResult
import com.fcbc.tesis.ui.viewmodel.ForumViewModel
import com.fcbc.tesis.ui.viewmodel.HistoryViewModel
import com.fcbc.tesis.ui.viewmodel.AuthViewModel
import com.fcbc.tesis.ui.viewmodel.AuthState
import com.fcbc.tesis.ui.viewmodel.ChatViewModel
import com.fcbc.tesis.ui.viewmodel.PublicProfileViewModel
import com.fcbc.tesis.domain.util.TechConstants
import kotlinx.coroutines.launch
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground
import com.fcbc.tesis.ui.tour.tourTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit,
    onNavigateToChatDetail: (String, String) -> Unit,
    onNavigateToPublicProfile: (String, String) -> Unit,
    viewModel: ForumViewModel = koinViewModel(),
    historyViewModel: HistoryViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    chatViewModel: ChatViewModel = koinViewModel(),
    publicProfileViewModel: PublicProfileViewModel = koinViewModel()
) {
    val posts = viewModel.pagedPosts.collectAsLazyPagingItems()
    val historyItems by historyViewModel.historyList.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val hasReachedEnd by viewModel.hasReachedEnd.collectAsStateWithLifecycle()
    val savedPostIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    
    var isWritingPost by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<ForumPost?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val chatErrorMessage by chatViewModel.errorMessage.collectAsStateWithLifecycle()
    
    LaunchedEffect(chatErrorMessage) {
        if (!chatErrorMessage.isNullOrEmpty()) {
            Toast.makeText(context, "Error de chat: $chatErrorMessage", Toast.LENGTH_LONG).show()
            chatViewModel.clearError()
        }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    CoffeeThemedBackground {
        if (isWritingPost) {
                PostCreationScreen(
                    initialPrediction = null,
                    initialImageUrl = null,
                    historyItems = historyItems,
                    onBack = { isWritingPost = false },
                    onConfirm = { editedContent, finalImageUrl, tag ->
                        val authorName = (authState as AuthState.Authenticated).userName
                        viewModel.addPost(authorName, "Consulta Manual", editedContent, listOf(tag), finalImageUrl)
                        isWritingPost = false
                    }
                )
            } else if (selectedPost != null) {
                ForumDetailScreen(
                    post = selectedPost!!,
                    onBack = { selectedPost = null },
                    onNavigateToProfile = onNavigateToProfile,
                    onStartChatWithUser = { uid, uname ->
                        chatViewModel.startChatWithUser(uid, uname) { roomId ->
                            onNavigateToChatDetail(roomId, uname)
                        }
                    }
                )
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(16.dp))
                            Text("Filtrar por Enfermedad", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            HorizontalDivider()
                            LazyColumn {
                                items(TechConstants.forumCategories) { category ->
                                    NavigationDrawerItem(
                                        label = { Text(text = category) },
                                        selected = (category == "Todas" && activeFilter == null) || category == activeFilter,
                                        onClick = {
                                            viewModel.setFilter(if (category == "Todas") null else category)
                                            coroutineScope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { 
                                    if (authState is AuthState.Authenticated) {
                                        isWritingPost = true 
                                    } else {
                                        onNavigateToProfile()
                                    }
                                },
                                containerColor = com.fcbc.tesis.ui.theme.RustOrange,
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Nueva Publicación")
                            }
                        }
                    ) { paddingValues ->
                        Column(
                            modifier = modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.Black, modifier = Modifier.clickable { coroutineScope.launch { drawerState.open() } })
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Foro Comunitario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                                        contentDescription = "Logo",
                                        modifier = Modifier.size(40.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Comparte experiencias y consultas con otros productores.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.tourTarget(10)
                                ) {
                                    item {
                                        Surface(
                                            color = if (activeFilter == null) MaterialTheme.colorScheme.primary else Color.White,
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.clickable { viewModel.setFilter(null) }
                                        ) {
                                            Text("Todos", color = if (activeFilter == null) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                        }
                                    }
                                    items(TechConstants.forumCategories.filter { it != "Todas" }) { cat ->
                                        Surface(
                                            color = if (activeFilter == cat) MaterialTheme.colorScheme.primary else Color.White,
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.clickable { viewModel.setFilter(cat) }
                                        ) {
                                            Text(cat, color = if (activeFilter == cat) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                        }
                                    }
                                }
                            }

                            if (posts.loadState.refresh is LoadState.Loading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else if (posts.itemCount == 0) {
                                EmptyForumView()
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        count = posts.itemCount,
                                        key = posts.itemKey { it.id },
                                        contentType = posts.itemContentType { "Post" }
                                    ) { index ->
                                        val post = posts[index]
                                        if (post != null) {
                                            PostCard(
                                                post = post,
                                                isSaved = savedPostIds.contains(post.id),
                                                modifier = if (index == 0) Modifier.tourTarget(11) else Modifier,
                                                currentUserId = (authState as? AuthState.Authenticated)?.userId,
                                                onClick = { selectedPost = post },
                                                onLikeClick = { viewModel.toggleLike(post.id) },
                                                onAuthorClick = { id, name -> onNavigateToPublicProfile(id, name) },
                                                onSaveClick = { viewModel.toggleSavePost(post.id) },
                                                onFollowClick = { id -> publicProfileViewModel.followUser(id) },
                                                onChatClick = { id, name ->
                                                    chatViewModel.startChatWithUser(id, name) { roomId ->
                                                        onNavigateToChatDetail(roomId, name)
                                                    }
                                                },
                                                onDeleteClick = { viewModel.deletePost(post.id) }
                                            )
                                        }
                                    }
                                    item {
                                        if (isLoadingMore) {
                                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            }
                                        } else if (hasReachedEnd && posts.itemCount > 0) {
                                            Text(
                                                "Son todos los posts, no hay más por cargar",
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                textAlign = TextAlign.Center,
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                        } else if (posts.itemCount > 0) {
                                            Button(
                                                onClick = { viewModel.loadMorePosts(posts.itemCount) },
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                            ) {
                                                Text("Cargar posteos más antiguos")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
fun CommunityLockScreen(onNavigateToProfile: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Acceso Exclusivo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Únete a miles de productores. Inicia sesión para ver consultas técnicas y compartir tus diagnósticos.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("IDENTIFICARME AHORA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyForumView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text("No hay conversaciones activas", fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("Sé el primero en pedir asesoría técnica.", fontSize = 14.sp, color = Color.LightGray)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCard(
    post: ForumPost,
    isSaved: Boolean = false,
    modifier: Modifier = Modifier,
    currentUserId: String? = null,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onAuthorClick: (String, String) -> Unit,
    onSaveClick: () -> Unit,
    onFollowClick: (String) -> Unit,
    onChatClick: (String, String) -> Unit,
    onDeleteClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().clickable { onClick() }
    ) {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences("post_likes", android.content.Context.MODE_PRIVATE)
        var isLiked by remember(post.id) { mutableStateOf(prefs.getBoolean("liked_${post.id}", false)) }

        Column(modifier = Modifier.padding(16.dp)) {
            var showAuthorMenu by remember { mutableStateOf(false) }

            Box {
                Row(
                    verticalAlignment = Alignment.Top, 
                    modifier = Modifier.clickable { 
                        if (post.userId != null) {
                            showAuthorMenu = true
                        } else {
                            Toast.makeText(context, "Usuario invitado sin perfil.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    if (!post.authorAvatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = "Avatar de ${post.author}",
                            modifier = Modifier.size(48.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.author,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Column {
                            val formattedTime = try {
                                val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.getDefault())
                                val parsedDate = parser.parse(post.timestamp)
                                val formatter = java.text.SimpleDateFormat("d/MM/yyyy h:mm a", java.util.Locale.getDefault())
                                formatter.format(parsedDate ?: java.util.Date())
                            } catch (e: Throwable) {
                                post.timestamp
                            }
                            Text(formattedTime, fontSize = 13.sp, color = Color.Gray)
                            if (!post.location.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(4.dp))
                                    Text(post.location, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    IconButton(onClick = { 
                        if (post.userId != null) {
                            showAuthorMenu = true
                        } else {
                            Toast.makeText(context, "Usuario invitado sin perfil.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray)
                    }
                }

                DropdownMenu(
                    expanded = showAuthorMenu,
                    onDismissRequest = { showAuthorMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ver perfil") },
                        onClick = {
                            showAuthorMenu = false
                            if (post.userId != null) onAuthorClick(post.userId, post.author)
                        }
                    )
                    if (post.userId == currentUserId) {
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = Color.Gray) },
                            onClick = {
                                showAuthorMenu = false
                                onDeleteClick()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Seguir") },
                            onClick = {
                                showAuthorMenu = false
                                if (post.userId != null) onFollowClick(post.userId)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hablar en privado") },
                            onClick = {
                                showAuthorMenu = false
                                if (post.userId != null) onChatClick(post.userId, post.author)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(post.cropContext, color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(post.content, color = Color.DarkGray, fontSize = 15.sp, lineHeight = 22.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)

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
                            Box(modifier = Modifier.width(160.dp).height(160.dp)) {
                                AsyncImage(
                                    model = resolveImageModel(url),
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

            if (post.diseaseTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    post.diseaseTags.forEach { tag ->
                        val isAyuda = tag.lowercase() == "ayuda" || tag.lowercase() == "urgente"
                        Surface(
                            color = if (isAyuda) Color(0xFFFDE8E8) else Color(0xFFE5E7EB),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = if (isAyuda) Color(0xFF9B1C1C) else Color.DarkGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // PREVIEW DEL ÚLTIMO COMENTARIO (UX SENIOR)
            if (post.lastCommentAuthor != null && post.lastCommentPreview != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${post.lastCommentAuthor}: ${post.lastCommentPreview}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                SocialButton(
                    icon = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                    tint = if (isLiked) com.fcbc.tesis.ui.theme.GreenPrimary else Color.Gray,
                    text = "${post.likes}",
                    onClick = { 
                        isLiked = !isLiked
                        onLikeClick() 
                    }
                )
                SocialButton(
                    icon = Icons.Default.ChatBubbleOutline, 
                    text = "${post.commentsCount}",
                    onClick = onClick
                )
                SocialButton(
                    icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, 
                    text = "Guardar",
                    onClick = onSaveClick
                )
            }
        }
    }
}

@Composable
fun SocialButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    text: String,
    tint: Color = Color.Gray,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.clickable { onClick() }.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = tint,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
