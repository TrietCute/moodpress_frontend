package com.example.moodpress.core.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.example.moodpress.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.Provides
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import javax.inject.Singleton


@ActivityScoped
class GoogleAuthManager @Inject constructor(
    @ActivityContext private val context: Context
) {
    suspend fun signIn(): String? {
        val credentialManager = CredentialManager.create(context)

        // Cấu hình yêu cầu đăng nhập Google
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            // Gọi hộp thoại đăng nhập
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Xử lý kết quả trả về
            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        // ĐÂY LÀ THỨ CHÚNG TA CẦN: ID TOKEN
                        val idToken = googleIdTokenCredential.idToken
                        Log.d("GoogleAuth", "Token: $idToken")
                        return idToken
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Lỗi đăng nhập: ${e.message}")
            null
        }
    }
}