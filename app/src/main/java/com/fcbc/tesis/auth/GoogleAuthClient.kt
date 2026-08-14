package com.fcbc.tesis.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.fcbc.tesis.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cliente para manejar la autenticación con Google a través de CredentialManager.
 */
class GoogleAuthClient(
    private val context: Context
) {
    private val WEB_CLIENT_ID = BuildConfig.GOOGLE_WEB_CLIENT_ID

    suspend fun signIn(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                handleSignIn(result)
            } catch (e: GetCredentialCancellationException) {
                Log.d("GoogleAuthClient", "Autenticación cancelada por el usuario")
                null
            } catch (e: GetCredentialException) {
                Log.e("GoogleAuthClient", "Error al obtener credencial: ${e.message}")
                null
            } catch (e: Exception) {
                Log.e("GoogleAuthClient", "Error desconocido: ${e.message}")
                null
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse): String? {
        val credential = result.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                return googleIdTokenCredential.idToken
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("GoogleAuthClient", "Error al parsear Google ID Token: ${e.message}")
            }
        } else {
            Log.e("GoogleAuthClient", "Credencial de tipo inesperado: ${credential.type}")
        }
        return null
    }
}
