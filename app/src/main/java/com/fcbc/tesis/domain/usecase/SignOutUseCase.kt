package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.AuthRepository
import com.fcbc.tesis.domain.util.Resource

class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return authRepository.signOut()
    }
}
