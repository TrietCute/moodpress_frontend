package com.example.moodpress.feature.chatbot.di

import com.example.moodpress.feature.chatbot.data.remote.api.ChatApiService
import com.example.moodpress.feature.chatbot.data.repository.ChatRepository
import com.example.moodpress.feature.chatbot.data.repository.ChatRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatRepositoryModule {

    // Liên kết Interface với bản Implementation
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ChatNetworkModule {

    // Cung cấp API Service từ Retrofit
    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }
}