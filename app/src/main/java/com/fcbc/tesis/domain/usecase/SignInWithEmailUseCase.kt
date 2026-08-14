package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.AuthRepository
import com.fcbc.tesis.domain.util.Resource

class SignInWithEmailUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<Unit> {
        return authRepository.signInWithEmail(email, password)
    }
}
