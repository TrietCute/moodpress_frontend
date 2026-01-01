package com.example.moodpress.core.network

import com.example.moodpress.core.utils.SessionManager
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor (
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val userId = sessionManager.fetchUserId()

        if (userId != null) {
            val newRequest = originalRequest.newBuilder()
                .header("X-User-ID", userId)
                .build()
            return chain.proceed(newRequest)
        }
        return chain.proceed(originalRequest)
    }
}