package com.example.moodpress.feature.chatbot.di

import com.example.moodpress.feature.chatbot.data.remote.api.ChatApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatNetworkModule {
    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }
}