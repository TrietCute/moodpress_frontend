package com.example.moodpress.feature.relax.data.remote.api

import com.example.moodpress.feature.relax.domain.model.RelaxSound
import retrofit2.http.GET

interface RelaxApiService {
    @GET("relax/sounds")
    suspend fun getRelaxSounds(): List<RelaxSound>
}