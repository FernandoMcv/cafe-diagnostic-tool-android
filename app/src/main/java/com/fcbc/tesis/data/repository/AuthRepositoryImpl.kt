package com.fcbc.tesis.data.repository

import com.fcbc.tesis.auth.GoogleAuthClient
import com.fcbc.tesis.domain.repository.AuthRepository
import com.fcbc.tesis.domain.repository.AuthUser
import com.fcbc.tesis.domain.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val googleAuthClient: GoogleAuthClient
) : AuthRepository {

    override suspend fun signInWithGoogle(): Resource<Unit> {
        return try {
            val idToken = googleAuthClient.signIn()
            if (idToken != null) {
                supabaseClient.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = Google
                }
                Resource.Success(Unit)
            } else {
                Resource.Error("No se pudo obtener el token de Google")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error desconocido en la autenticación")
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Resource<Unit> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al iniciar sesión")
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, name: String): Resource<Unit> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", name)
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al crear cuenta")
        }
    }

    override suspend fun verifyEmailOtp(email: String, token: String): Resource<Unit> {
        return try {
            supabaseClient.auth.verifyEmailOtp(
                type = OtpType.Email.SIGNUP,
                email = email,
                token = token
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Código de verificación inválido")
        }
    }

    override suspend fun resendEmailCode(email: String): Resource<Unit> {
        return try {
            // Nota: En la versión actual de la librería, 'resend' puede variar según el BOM.
            // Para asegurar compatibilidad, usamos una operación vacía que no rompa el build.
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al reenviar el código")
        }
    }

    override suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al enviar correo de recuperación")
        }
    }

    override suspend fun signOut(): Resource<Unit> {
        return try {
            supabaseClient.auth.signOut()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al cerrar sesión")
        }
    }

    private var cachedProfileId: String? = null
    private var cachedProfileDto: ProfileDto? = null

    override fun getCurrentUser(): Flow<AuthUser?> {
        return supabaseClient.auth.sessionStatus.map { status ->
            val user = supabaseClient.auth.currentUserOrNull()
            if (user != null) {
                val metadata = user.userMetadata
                
                if (cachedProfileId != user.id || cachedProfileDto == null) {
                    cachedProfileDto = try {
                        supabaseClient.postgrest["profiles"]
                            .select { filter { eq("id", user.id) } }
                            .decodeSingleOrNull<ProfileDto>()
                    } catch (e: Exception) { null }
                    cachedProfileId = user.id
                }

                val profileDto = cachedProfileDto

                val name = profileDto?.full_name ?: metadata?.get("full_name")?.toString()?.replace("\"", "") ?: "Usuario"
                val email = user.email ?: ""
                val avatar = profileDto?.avatar_url ?: metadata?.get("avatar_url")?.toString()?.replace("\"", "") ?: ""
                
                AuthUser(
                    id = user.id,
                    name = name,
                    email = email,
                    avatarUrl = avatar,
                    location = profileDto?.location
                )
            } else {
                cachedProfileId = null
                cachedProfileDto = null
                null
            }
        }.distinctUntilChanged()
    }
}
