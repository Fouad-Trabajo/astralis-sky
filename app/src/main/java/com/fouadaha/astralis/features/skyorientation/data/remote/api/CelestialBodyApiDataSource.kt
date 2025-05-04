package com.fouadaha.astralis.features.skyorientation.data.remote.api

import com.fouadaha.astralis.core.data.remote.apiCall
import com.fouadaha.astralis.features.skyorientation.domain.CelestialBody
import org.koin.core.annotation.Single

@Single
class CelestialBodyApiDataSource(private val celestialBodyApiService: CelestialBodyApiService) {

    suspend fun getAllBodies(): Result<List<CelestialBody?>> {
        return apiCall { celestialBodyApiService.getAllBodies() }.map { celestialBodyApiModel ->
            celestialBodyApiModel.bodies.map { it.toDomain() }
        }
    }
}