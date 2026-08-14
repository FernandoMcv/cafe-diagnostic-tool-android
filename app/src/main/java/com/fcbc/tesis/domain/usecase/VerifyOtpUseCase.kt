package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.AuthRepository
import com.fcbc.tesis.domain.util.Resource

class VerifyOtpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, code: String): Resource<Unit> {
        return authRepository.verifyEmailOtp(email, code)
    }
}
