package com.fcbc.tesis.domain.repository

import com.fcbc.tesis.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signInWithGoogle(): Resource<Unit>
    suspend fun signInWithEmail(email: String, password: String): Resource<Unit>
    suspend fun signUpWithEmail(email: String, password: String, name: String): Resource<Unit>
    suspend fun verifyEmailOtp(email: String, token: String): Resource<Unit>
    suspend fun resendEmailCode(email: String): Resource<Unit>
    suspend fun resetPassword(email: String): Resource<Unit>
    suspend fun signOut(): Resource<Unit>
    fun getCurrentUser(): Flow<AuthUser?>
}

data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String,
    val location: String? = null
)
