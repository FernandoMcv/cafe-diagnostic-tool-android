package com.fcbc.tesis.ui.screen

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcbc.tesis.domain.util.TechConstants
import kotlinx.coroutines.launch
import com.fcbc.tesis.ui.tour.tourTarget
import com.fcbc.tesis.ui.viewmodel.HistoryViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

import androidx.compose.material.icons.filled.AccountCircle
import com.fcbc.tesis.ui.viewmodel.AuthState
import com.fcbc.tesis.ui.viewmodel.AuthViewModel
import com.fcbc.tesis.ui.theme.CoffeeThemedBackground

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToFollowing: () -> Unit,
    onNavigateToSavedPosts: () -> Unit,
    historyViewModel: HistoryViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    userViewModel: com.fcbc.tesis.ui.viewmodel.UserInteractionsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val historyItems by historyViewModel.historyList.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val otpTimer by authViewModel.otpTimer.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showSignInDialog by remember { mutableStateOf(false) }
    var showSignUpDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    
    var termsAccepted by remember { mutableStateOf(false) }
    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val executeOrRequireTerms: (() -> Unit) -> Unit = { action ->
        if (termsAccepted) {
            action()
        } else {
            pendingAuthAction = action
            showTermsDialog = true
            coroutineScope.launch { snackbarHostState.showSnackbar("Para continuar, acepta nuestros Términos y Condiciones.") }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated || authState is AuthState.Success) {
            showSignInDialog = false
            showSignUpDialog = false
            showResetPasswordDialog = false
            showOtpDialog = false
        }
        if (authState is AuthState.AwaitingVerification) {
            showSignUpDialog = false
            showOtpDialog = true
        }
    }

    CoffeeThemedBackground {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.fcbc.tesis.R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auth Section
            when (val state = authState) {
                is AuthState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AuthState.Authenticated -> {
                    Box(modifier = Modifier.tourTarget(15)) {
                        AuthenticatedProfileView(
                            name = state.userName,
                            email = state.userEmail,
                            location = state.location,
                            onEditName = { showEditNameDialog = true },
                            diagnosticsCount = historyItems.size,
                            postsCount = 0
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Comunidad & Guardados Cards
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToFollowing() }.tourTarget(17),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = com.fcbc.tesis.ui.theme.GreenPrimary, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.People, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Comunidad", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(Modifier.height(4.dp))
                                Text("Gestiona tus seguidores y las parcelas que sigues.", fontSize = 13.sp, color = Color.DarkGray)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToSavedPosts() }.tourTarget(16),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = com.fcbc.tesis.ui.theme.WarningOrange, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Guardados", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(Modifier.height(4.dp))
                                Text("Revisa los artículos técnicos y guías que has guardado.", fontSize = 13.sp, color = Color.DarkGray)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                is AuthState.Success -> {
                    SuccessInfoView(
                        message = state.message,
                        onDismiss = { authViewModel.clearState() }
                    )
                }
                is AuthState.AwaitingVerification -> {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Esperando verificación...", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                else -> {
                    UnauthenticatedProfileView(
                        onSignInGoogle = { executeOrRequireTerms { authViewModel.signInWithGoogle() } },
                        onSignInEmail = { executeOrRequireTerms { showSignInDialog = true } },
                        onSignUpEmail = { executeOrRequireTerms { showSignUpDialog = true } },
                        onSignInGuest = { executeOrRequireTerms { authViewModel.signInAsGuest() } },
                        errorMessage = if (state is AuthState.Error) state.message else null
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Gestión de Evidencias", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            SettingsItem(
                                icon = Icons.Default.Share,
                                title = "Exportar Dataset (CSV)",
                                subtitle = "Guardar reporte en carpeta Descargas",
                                iconColor = com.fcbc.tesis.ui.theme.GreenPrimary,
                                onClick = { exportarDatosCSV(context, historyItems) }
                            )
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsItem(
                                icon = Icons.Default.Delete,
                                title = "Reiniciar Base de Datos",
                                subtitle = "Eliminar todo el historial de la app",
                                iconColor = com.fcbc.tesis.ui.theme.ErrorRed,
                                onClick = {
                                    historyViewModel.clearAll()
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Base de datos reiniciada") }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().tourTarget(18)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Settings,
                        title = "Configuración de la Cuenta",
                        subtitle = "",
                        iconColor = Color.DarkGray,
                        onClick = { }
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Información Legal",
                        subtitle = "",
                        iconColor = Color.DarkGray,
                        onClick = { showTermsDialog = true }
                    )
                }
            }

            if (authState is AuthState.Authenticated) {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { authViewModel.signOut() },
                    colors = ButtonDefaults.buttonColors(containerColor = com.fcbc.tesis.ui.theme.ErrorRed),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.height(56.dp).fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // DIALOGS ...
    if (showSignInDialog) {
        AuthDialog(
            title = "Iniciar Sesión",
            confirmButtonText = "Entrar",
            isLoading = authState is AuthState.Loading,
            onDismiss = { showSignInDialog = false },
            onConfirm = { email, pass, _ -> authViewModel.signInWithEmail(email, pass) },
            onForgotPassword = {
                showSignInDialog = false
                showResetPasswordDialog = true
            },
            isSignUp = false
        )
    }

    if (showResetPasswordDialog) {
        ResetPasswordDialog(
            isLoading = authState is AuthState.Loading,
            onDismiss = { showResetPasswordDialog = false },
            onConfirm = { email -> authViewModel.resetPassword(email) }
        )
    }

    if (showSignUpDialog) {
        AuthDialog(
            title = "Crear Cuenta",
            confirmButtonText = "Registrarme",
            isLoading = authState is AuthState.Loading,
            onDismiss = { showSignUpDialog = false },
            onConfirm = { email, pass, name -> authViewModel.signUpWithEmail(email, pass, name ?: "") },
            isSignUp = true
        )
    }

    if (showOtpDialog && authState is AuthState.AwaitingVerification) {
        val email = (authState as AuthState.AwaitingVerification).email
        OtpVerificationDialog(
            email = email,
            isLoading = authState is AuthState.Loading,
            timer = otpTimer,
            onDismiss = { showOtpDialog = false; authViewModel.clearState() },
            onConfirm = { code -> authViewModel.verifyOtp(email, code) },
            onResend = { authViewModel.resendCode(email) }
        )
    }

    if (showTermsDialog) {
        TermsAndConditionsDialog(
            onDismiss = { 
                showTermsDialog = false
                pendingAuthAction = null
            },
            onAccept = {
                termsAccepted = true
                showTermsDialog = false
                pendingAuthAction?.invoke()
                pendingAuthAction = null
            }
        )
    }

    if (showEditNameDialog && authState is AuthState.Authenticated) {
        val authStateAuth = authState as AuthState.Authenticated
        var newName by rememberSaveable { mutableStateOf(authStateAuth.userName) }
        var newLocation by rememberSaveable { mutableStateOf(authStateAuth.location ?: "") }
        var isUpdating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = if (isUpdating) ({}) else ({ showEditNameDialog = false }),
            title = { Text("Editar Perfil", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nuevo nombre visible") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUpdating
                    )
                    OutlinedTextField(
                        value = newLocation,
                        onValueChange = { newLocation = it },
                        label = { Text("Ubicación (Finca, Cultivo)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUpdating
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUpdating = true
                        userViewModel.updateProfileData(newName, newLocation) {
                            isUpdating = false
                            showEditNameDialog = false
                            coroutineScope.launch { snackbarHostState.showSnackbar("Perfil actualizado correctamente.") }
                        }
                    },
                    enabled = newName.isNotBlank() && !isUpdating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }, enabled = !isUpdating) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun TermsAndConditionsDialog(onDismiss: () -> Unit, onAccept: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Términos y Condiciones",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "1. Responsabilidad del Usuario\n" +
                               "El usuario es el único y exclusivo responsable de cualquier fotografía, comentario, diagnóstico o contenido (UGC - User Generated Content) que publique en el Foro Comunitario o en los Chats de esta aplicación. El creador de la aplicación actúa únicamente como proveedor tecnológico.\n\n" +
                               "2. Propiedad Intelectual\n" +
                               "Al subir imágenes de cultivos al foro público, el usuario garantiza que posee los derechos sobre las mismas. La aplicación no se hace responsable por infracciones cometidas por los usuarios.\n\n" +
                               "3. Exactitud de la Inteligencia Artificial\n" +
                               "Los resultados entregados por el modelo de IA son de carácter orientativo y educativo. No reemplazan el criterio de un ingeniero agrónomo profesional. La aplicación de agroquímicos basada en estos diagnósticos es responsabilidad absoluta del agricultor.\n\n" +
                               "4. Moderación de Contenido\n" +
                               "Nos reservamos el estricto derecho de eliminar, sin previo aviso, cualquier contenido o cuenta que publique información maliciosa, engañosa o no relacionada con el cultivo de café.\n\n" +
                               "5. Privacidad de Datos\n" +
                               "Los datos personales y de sesión están protegidos mediante protocolos de autenticación en la nube. No compartimos tu información con terceros con fines comerciales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        lineHeight = 22.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aceptar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AuthenticatedProfileView(
    name: String, 
    email: String,
    location: String?,
    onEditName: () -> Unit,
    diagnosticsCount: Int,
    postsCount: Int
) {
    Column {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                // Top Right wave effect mockup
                Surface(
                    color = com.fcbc.tesis.ui.theme.GreenPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(bottomStart = 100.dp),
                    modifier = Modifier.align(Alignment.TopEnd).size(100.dp)
                ) {}

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = com.fcbc.tesis.ui.theme.GreenPrimary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Avatar",
                                tint = com.fcbc.tesis.ui.theme.GreenPrimary,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onEditName, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar nombre", tint = com.fcbc.tesis.ui.theme.GreenPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (!location.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Ubicación", tint = com.fcbc.tesis.ui.theme.GreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFEBEBEB),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Productor Orgánico Certificado",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Impacto", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F5F0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(diagnosticsCount.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = com.fcbc.tesis.ui.theme.GreenPrimary)
                    Text("Diagnósticos", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F5F0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(postsCount.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = com.fcbc.tesis.ui.theme.WarningOrange)
                    Text("Publicaciones", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun TechnicalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.DarkGray,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Composable
fun SuccessInfoView(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "¡Acción Exitosa!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF1B5E20),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OtpVerificationDialog(
    email: String,
    isLoading: Boolean,
    timer: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onResend: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = if (isLoading) ({}) else onDismiss,
        title = { Text("Verificar Identidad", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ingresa el código de 6 dígitos enviado a:", style = MaterialTheme.typography.bodyMedium)
                Text(email, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = { Text("Código OTP") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    placeholder = { Text("000000") },
                    shape = RoundedCornerShape(12.dp)
                )
                TextButton(
                    onClick = onResend,
                    enabled = timer == 0 && !isLoading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (timer > 0) "Reenviar en ${timer}s" else "Reenviar código")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.length == 6 && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("Verificar Ahora")
            }
        }
    )
}

@Composable
fun AuthDialog(
    title: String,
    confirmButtonText: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit,
    onForgotPassword: (() -> Unit)? = null,
    isSignUp: Boolean
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = if (isLoading) ({}) else onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (!isSignUp && onForgotPassword != null) {
                    TextButton(
                        onClick = onForgotPassword,
                        modifier = Modifier.align(Alignment.End),
                        enabled = !isLoading
                    ) {
                        Text("¿Olvidaste tu contraseña?", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email, password, if (isSignUp) name else null) },
                enabled = email.isNotBlank() && password.length >= 6 && !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp).fillMaxWidth(0.5f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(confirmButtonText, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancelar", color = Color.Gray) }
        }
    )
}

@Composable
fun ResetPasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = if (isLoading) ({}) else onDismiss,
        title = { Text("Recuperar Cuenta", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ingresa tu correo registrado para enviarte un enlace seguro.")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email) },
                enabled = email.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Enviar Enlace", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancelar", color = Color.Gray) }
        }
    )
}

@Composable
fun UnauthenticatedProfileView(
    onSignInGoogle: () -> Unit,
    onSignInEmail: () -> Unit,
    onSignUpEmail: () -> Unit,
    onSignInGuest: () -> Unit = {},
    errorMessage: String?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            // Top Right wave effect mockup
            Surface(
                color = com.fcbc.tesis.ui.theme.GreenPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(bottomStart = 100.dp),
                modifier = Modifier.align(Alignment.TopEnd).size(100.dp)
            ) {}

            Column(
                modifier = Modifier.padding(top = 32.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = com.fcbc.tesis.ui.theme.GreenPrimary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = com.fcbc.tesis.ui.theme.GreenPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Únete a la Comunidad",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Inicia sesión para compartir diagnósticos y contactar expertos.",
                    fontSize = 15.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onSignInGoogle,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.fcbc.tesis.ui.theme.GreenPrimary),
                    elevation = ButtonDefaults.buttonElevation(2.dp)
                ) {
                    Text(
                        text = "CONTINUAR CON GOOGLE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onSignInEmail,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, com.fcbc.tesis.ui.theme.GreenPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "INICIAR CON CORREO",
                        fontWeight = FontWeight.Bold,
                        color = com.fcbc.tesis.ui.theme.GreenPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                TextButton(onClick = onSignUpEmail, modifier = Modifier.padding(top = 16.dp)) {
                    Text("¿Aún no tienes cuenta? Regístrate aquí", color = com.fcbc.tesis.ui.theme.GreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(onClick = onSignInGuest, modifier = Modifier.fillMaxWidth()) {
                    Text("Ingresar como Especialista (Demo)", color = Color.Gray, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = iconColor.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}

fun exportarDatosCSV(context: Context, historyItems: List<com.fcbc.tesis.domain.model.HistoryItem>) {
    val fileName = "Reporte_Cafe_${System.currentTimeMillis()}.csv"

    try {
        var outputStream: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) outputStream = resolver.openOutputStream(uri)
        } else {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileName)
            outputStream = FileOutputStream(file)
        }

        outputStream?.use { stream ->
            stream.write(0xef); stream.write(0xbb); stream.write(0xbf)
            val writer = java.io.OutputStreamWriter(stream)
            writer.append("ID,ENFERMEDAD,CONFIANZA (%),FECHA,HORA\n")
            historyItems.forEach { item ->
                val confPorcentaje = (item.confidence * 100).toInt()
                writer.append("${item.id},${item.diseaseName},$confPorcentaje%,${item.date},${item.time}\n")
            }
            writer.flush(); writer.close()
        }
        Toast.makeText(context, "Guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
