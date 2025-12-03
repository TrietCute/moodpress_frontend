package com.example.moodpress.feature.journal.di

import com.example.moodpress.feature.journal.data.remote.api.JournalApiService
import com.example.moodpress.feature.journal.data.repository.JournalRepository
import com.example.moodpress.feature.journal.data.repository.JournalRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class JournalModule {
    @Binds
    @Singleton
    abstract fun bindJournalRepository(
        impl: JournalRepositoryImpl
    ): JournalRepository
}

@Module
@InstallIn(SingletonComponent::class)
object JournalNetworkModule {

    @Provides
    @Singleton
    fun provideJournalApiService(retrofit: Retrofit): JournalApiService {
        return retrofit.create(JournalApiService::class.java)
    }
}