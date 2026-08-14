package com.fcbc.tesis.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcbc.tesis.domain.usecase.*
import com.fcbc.tesis.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userName: String, val userEmail: String, val avatarUrl: String, val userId: String = "", val location: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
    data class Success(val message: String) : AuthState()
    data class AwaitingVerification(val email: String) : AuthState() // Nuevo estado para OTP
}

class AuthViewModel(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val resendCodeUseCase: ResendCodeUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val context: Context
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _otpTimer = MutableStateFlow(0)
    val otpTimer: StateFlow<Int> = _otpTimer.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Flag atómico de guardia: impide que se lancen llamadas concurrentes
     * de auto-login cuando el Flow de sesión emite null varias veces seguidas.
     * AtomicBoolean es thread-safe sin necesidad de synchronized.
     */
    private val isSigningInAsGuest = AtomicBoolean(false)

    private fun startOtpTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _otpTimer.value = 60
            while (_otpTimer.value > 0) {
                delay(1000)
                _otpTimer.value -= 1
            }
        }
    }

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            getCurrentUserUseCase()
                .catch { e ->
                    // Si el SDK de Supabase Auth lanza una excepción (token inválido,
                    // timeout, etc.) el Flow termina con error. Sin este catch, la
                    // excepción no manejada mataría la app.
                    e.printStackTrace()
                    _authState.value = AuthState.Idle
                }
                .collectLatest { user ->
                    if (user != null) {
                        _authState.value = AuthState.Authenticated(user.name, user.email, user.avatarUrl ?: "", user.id, user.location)
                    } else {
                        signInAsGuest()
                    }
                }
        }
    }

    fun clearState() {
        _authState.value = AuthState.Idle
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val resource = loginWithGoogleUseCase()
            if (resource is Resource.Error) {
                _authState.value = AuthState.Error(resource.message ?: "Error desconocido")
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val resource = signInWithEmailUseCase(email, password)
            if (resource is Resource.Error) {
                _authState.value = AuthState.Error(resource.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val resource = signUpWithEmailUseCase(email, password, name)
            when (resource) {
                is Resource.Success -> {
                    _authState.value = AuthState.AwaitingVerification(email)
                    startOtpTimer()
                }
                is Resource.Error -> {
                    _authState.value = AuthState.Error(resource.message ?: "Error al crear cuenta")
                }
                else -> _authState.value = AuthState.Idle
            }
        }
    }

    fun verifyOtp(email: String, code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val resource = verifyOtpUseCase(email, code)
            if (resource is Resource.Error) {
                _authState.value = AuthState.Error(resource.message ?: "Código inválido")
                // Mantenemos el estado de espera para reintentar
                _authState.value = AuthState.AwaitingVerification(email)
            }
        }
    }

    fun resendCode(email: String) {
        viewModelScope.launch {
            resendCodeUseCase(email)
            startOtpTimer()
            // No cambiamos estado a Loading para no quitar el diálogo de OTP
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val resource = resetPasswordUseCase(email)
            when (resource) {
                is Resource.Success -> {
                    _authState.value = AuthState.Success("Enlace de recuperación enviado. Revisa tu bandeja de entrada.")
                }
                is Resource.Error -> {
                    _authState.value = AuthState.Error(resource.message ?: "Error al enviar correo")
                }
                else -> _authState.value = AuthState.Idle
            }
        }
    }

    /**
     * Auto-login transparente para usuarios no autenticados.
     * Estándar de industria:
     *  - Flag atómico para evitar llamadas concurrentes.
     *  - withTimeoutOrNull(10s) para no dejar la UI en Loading indefinidamente.
     *  - En timeout o error: estado Idle, NO Error — la app sigue usable offline.
     */
    fun signInAsGuest() {
        // Guardia: si ya hay un intento en curso, no lanzar otro
        if (!isSigningInAsGuest.compareAndSet(false, true)) return

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val resource = withTimeoutOrNull(10_000L) {
                    signInWithEmailUseCase("demo@tesis.com", "eval1234")
                }
                when {
                    resource == null -> {
                        // Timeout de red (>10s): la app sigue funcional en modo offline
                        _authState.value = AuthState.Idle
                    }
                    resource is Resource.Error -> {
                        // Error de credenciales o red: no mostramos error al usuario
                        // porque es un login automático, no una acción explícita del usuario
                        _authState.value = AuthState.Idle
                    }
                    // Resource.Success: el Flow de sesión detectará el nuevo usuario
                    // y actualizará el estado a Authenticated automáticamente
                }
            } catch (e: Exception) {
                // Excepción inesperada: mantener la app funcional
                e.printStackTrace()
                _authState.value = AuthState.Idle
            } finally {
                // Liberar el flag SIEMPRE, incluso si hubo excepción
                isSigningInAsGuest.set(false)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            signOutUseCase()
        }
    }
}
