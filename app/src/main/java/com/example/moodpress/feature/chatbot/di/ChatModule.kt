package com.example.moodpress.feature.chatbot.di

import com.example.moodpress.feature.chatbot.data.repository.ChatRepository
import com.example.moodpress.feature.chatbot.data.repository.ChatRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository
}