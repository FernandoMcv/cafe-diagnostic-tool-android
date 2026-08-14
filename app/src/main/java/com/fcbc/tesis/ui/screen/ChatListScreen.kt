package com.fcbc.tesis.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcbc.tesis.domain.model.ChatRoom
import com.fcbc.tesis.ui.viewmodel.AuthState
import com.fcbc.tesis.ui.viewmodel.AuthViewModel
import com.fcbc.tesis.ui.viewmodel.ChatViewModel
import org.koin.androidx.compose.koinViewModel
import com.fcbc.tesis.ui.tour.tourTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    chatViewModel: ChatViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val chatRooms by chatViewModel.chatRooms.collectAsStateWithLifecycle()
    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()

    Surface(color = com.fcbc.tesis.ui.theme.CreamBackground, modifier = modifier.fillMaxSize()) {
        if (authState !is AuthState.Authenticated) {
            CommunityLockScreen(onNavigateToProfile)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Mensajes Directos",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                
                // Search Bar
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Buscar contacto...", color = Color.Gray) },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                if (isLoading && chatRooms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (chatRooms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes conversaciones activas.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize().tourTarget(13)
                    ) {
                        items(chatRooms) { room ->
                            ChatRoomCard(room = room, onClick = {
                                onNavigateToChat(room.id, room.participantName)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRoomCard(room: ChatRoom, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = com.fcbc.tesis.ui.theme.GreenPrimary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = com.fcbc.tesis.ui.theme.GreenPrimary, modifier = Modifier.size(32.dp))
                }
            }
            // Mock online indicator for the first item or logic based on room
            if (room.participantName.contains("Maria")) {
                Surface(
                    shape = CircleShape,
                    color = com.fcbc.tesis.ui.theme.GreenSecondaryDark,
                    border = androidx.compose.foundation.BorderStroke(2.dp, com.fcbc.tesis.ui.theme.CreamBackground),
                    modifier = Modifier.size(14.dp).align(Alignment.BottomEnd).offset(x = (-2).dp, y = (-2).dp)
                ) {}
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(room.participantName, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
                Text("10:30 AM", color = com.fcbc.tesis.ui.theme.GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                room.lastMessage ?: "Empieza a chatear...",
                color = Color.DarkGray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
