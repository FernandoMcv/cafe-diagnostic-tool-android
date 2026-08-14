package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.AuthRepository
import com.fcbc.tesis.domain.util.Resource

class SignUpWithEmailUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, name: String): Resource<Unit> {
        return authRepository.signUpWithEmail(email, password, name)
    }
}
