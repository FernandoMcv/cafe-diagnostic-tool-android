package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.AuthRepository
import com.fcbc.tesis.domain.util.Resource

class ResendCodeUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Resource<Unit> {
        return authRepository.resendEmailCode(email)
    }
}
