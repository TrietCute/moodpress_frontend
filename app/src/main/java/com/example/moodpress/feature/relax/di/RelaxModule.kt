package com.example.moodpress.feature.relax.di

import com.example.moodpress.feature.relax.data.remote.api.RelaxApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RelaxModule {
    @Provides
    @Singleton
    fun provideRelaxApiService(retrofit: Retrofit): RelaxApiService {
        return retrofit.create(RelaxApiService::class.java)
    }
}