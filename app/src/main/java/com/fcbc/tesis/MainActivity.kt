package com.fcbc.tesis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Group
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.fcbc.tesis.ui.theme.ApptessTheme
import com.fcbc.tesis.ui.theme.CreamBackground
import com.fcbc.tesis.ui.screen.CafeDetectionScreen
import com.fcbc.tesis.ui.screen.ForumScreen
import com.fcbc.tesis.ui.screen.HistoryScreen
import com.fcbc.tesis.ui.screen.ProfileScreen

import com.fcbc.tesis.ui.screen.ChatListScreen
import com.fcbc.tesis.ui.screen.ChatDetailScreen
import com.fcbc.tesis.ui.screen.FollowingListScreen
import com.fcbc.tesis.ui.screen.PublicProfileScreen
import com.fcbc.tesis.ui.screen.SavedPostsScreen
import com.fcbc.tesis.ui.viewmodel.ChatViewModel
import com.fcbc.tesis.ui.tour.AppTourOverlay
import com.fcbc.tesis.ui.tour.TourManager
import com.fcbc.tesis.ui.tour.tourTarget
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApptessTheme {
                ApptessApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun ApptessApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var activeChatRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeChatParticipantName by rememberSaveable { mutableStateOf<String?>(null) }
    
    var activePublicProfileUserId by remember { mutableStateOf<String?>(null) }
    var activePublicProfileUserName by remember { mutableStateOf<String?>(null) }
    var isCameraOpen by remember { mutableStateOf(false) }
    var showFollowingList by rememberSaveable { mutableStateOf(false) }
    var showSavedPosts by rememberSaveable { mutableStateOf(false) }

    val chatViewModel: ChatViewModel = koinViewModel()

    val navigateToProfile = { currentDestination = AppDestinations.PROFILE }
    val navigateToChatDetail = { roomId: String, participantName: String ->
        activeChatRoomId = roomId
        activeChatParticipantName = participantName
    }
    val backFromChatDetail = {
        activeChatRoomId = null
    }

    val currentTourStep = TourManager.currentStep.value
    LaunchedEffect(currentTourStep) {
        if (currentTourStep > 0) {
            activeChatRoomId = null
            activePublicProfileUserId = null
            showFollowingList = false
            showSavedPosts = false
        }
        when (currentTourStep) {
            0, 1, 2, 3, 4, 5 -> currentDestination = AppDestinations.HOME
            6, 7, 8 -> currentDestination = AppDestinations.HISTORY
            9, 10, 11 -> currentDestination = AppDestinations.FORUM
            12, 13 -> currentDestination = AppDestinations.CHATS
            14, 15, 16, 17, 18 -> currentDestination = AppDestinations.PROFILE
        }
    }

    val navigateToPublicProfile = { userId: String, userName: String ->
        activePublicProfileUserId = userId
        activePublicProfileUserName = userName
    }

    if (activeChatRoomId != null && activeChatParticipantName != null) {
        ChatDetailScreen(
            roomId = activeChatRoomId!!,
            participantName = activeChatParticipantName!!,
            onBack = backFromChatDetail
        )
        return
    }

    if (activePublicProfileUserId != null && activePublicProfileUserName != null) {
        PublicProfileScreen(
            userId = activePublicProfileUserId!!,
            userName = activePublicProfileUserName!!,
            onBack = { activePublicProfileUserId = null },
            onNavigateToChat = { uid, uname ->
                chatViewModel.startChatWithUser(uid, uname) { roomId ->
                    navigateToChatDetail(roomId, uname)
                }
            }
        )
        return
    }

    if (showFollowingList) {
        FollowingListScreen(
            onBack = { showFollowingList = false },
            onUserClick = navigateToPublicProfile
        )
        return
    }

    if (showSavedPosts) {
        SavedPostsScreen(
            onBack = { showSavedPosts = false }
        )
        return
    }

    val canGoBack = activeChatRoomId != null || 
                    activePublicProfileUserId != null || 
                    showFollowingList || 
                    showSavedPosts || 
                    isCameraOpen ||
                    currentDestination != AppDestinations.HOME

    BackHandler(enabled = canGoBack) {
        when {
            activeChatRoomId != null -> backFromChatDetail()
            activePublicProfileUserId != null -> activePublicProfileUserId = null
            showFollowingList -> showFollowingList = false
            showSavedPosts -> showSavedPosts = false
            isCameraOpen -> isCameraOpen = false
            currentDestination != AppDestinations.HOME -> currentDestination = AppDestinations.HOME
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            layoutType = if (isCameraOpen && currentDestination == AppDestinations.HOME) NavigationSuiteType.None else NavigationSuiteType.NavigationBar,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = Color.White,
                navigationRailContainerColor = Color.White
            ),
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    val stepMapping = when(it) {
                        AppDestinations.HOME -> 5
                        AppDestinations.HISTORY -> 6
                        AppDestinations.FORUM -> 9
                        AppDestinations.CHATS -> 12
                        AppDestinations.PROFILE -> 14
                    }
                    item(
                        icon = {
                            Icon(
                                it.icon,
                                contentDescription = it.label,
                                modifier = Modifier.tourTarget(stepMapping)
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        CafeDetectionScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToProfile = navigateToProfile,
                            isCameraOpen = isCameraOpen,
                            onCameraOpenChange = { isCameraOpen = it }
                        )
                    }
                    AppDestinations.HISTORY -> {
                        HistoryScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToProfile = navigateToProfile
                        )
                    }
                    AppDestinations.FORUM -> {
                        ForumScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToProfile = navigateToProfile,
                            onNavigateToChatDetail = navigateToChatDetail,
                            onNavigateToPublicProfile = navigateToPublicProfile
                        )
                    }
                    AppDestinations.CHATS -> {
                        ChatListScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToProfile = navigateToProfile,
                            onNavigateToChat = navigateToChatDetail
                        )
                    }
                    AppDestinations.PROFILE -> {
                        ProfileScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToFollowing = { showFollowingList = true },
                            onNavigateToSavedPosts = { showSavedPosts = true }
                        )
                    }
                }
            }
        }
        
        AppTourOverlay()
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Inicio", Icons.Outlined.Home),
    HISTORY("Historial", Icons.Outlined.History),
    FORUM("Social", Icons.Outlined.Group),
    CHATS("Mensajes", Icons.Outlined.ChatBubbleOutline),
    PROFILE("Perfil", Icons.Outlined.Person),
}
