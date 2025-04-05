package com.fouadaha.astralis.features.celestialobodies.di

import com.fouadaha.astralis.core.data.local.AstralisDataBase
import com.fouadaha.astralis.features.celestialobodies.data.local.db.CelestialBodiesDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan
class CelestialBodyModule {

    @Single
    fun provideCelestialBodiesDao(db: AstralisDataBase): CelestialBodiesDao {
        return db.celestialBodiesDao()
    }
}