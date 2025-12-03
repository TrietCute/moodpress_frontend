package com.example.moodpress.feature.stats.di

import com.example.moodpress.feature.stats.data.remote.api.StatsApiService
import com.example.moodpress.feature.stats.data.repository.StatsRepositoryImpl
import com.example.moodpress.feature.stats.data.repository.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StatsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object StatsNetworkModule {
    @Provides
    @Singleton
    fun provideStatsApiService(retrofit: Retrofit): StatsApiService {
        return retrofit.create(StatsApiService::class.java)
    }
}