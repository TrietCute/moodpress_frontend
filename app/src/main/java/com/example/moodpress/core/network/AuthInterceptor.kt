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

        // Lấy User ID từ SessionManager
        val userId = sessionManager.fetchUserId()

        // Nếu có User ID, thêm nó vào header "X-User-ID"
        if (userId != null) {
            val newRequest = originalRequest.newBuilder()
                .header("X-User-ID", userId) // <-- Header mới
                .build()
            return chain.proceed(newRequest)
        }

        // Nếu không có ID (ví dụ: app vừa cài, chưa qua Splash)
        // thì cứ gửi request gốc
        return chain.proceed(originalRequest)
    }
}