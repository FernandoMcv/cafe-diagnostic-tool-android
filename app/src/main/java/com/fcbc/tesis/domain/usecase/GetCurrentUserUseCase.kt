package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.AuthRepository
import com.fcbc.tesis.domain.repository.AuthUser
import kotlinx.coroutines.flow.Flow

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<AuthUser?> {
        return authRepository.getCurrentUser()
    }
}
