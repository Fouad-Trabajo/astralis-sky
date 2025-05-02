package com.fouadaha.astralis.features.skyorientation.di

import com.fouadaha.astralis.features.skyorientation.data.remote.api.CelestialBodyApiService
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import retrofit2.Retrofit

@Module
@ComponentScan
class SkyOrientationModule {

    @Single
    fun provideCelestialBodyService(retrofit: Retrofit): CelestialBodyApiService =
        retrofit.create(CelestialBodyApiService::class.java)
}