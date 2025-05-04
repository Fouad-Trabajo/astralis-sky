package com.fouadaha.astralis.features.skyorientation.domain

import org.koin.core.annotation.Single

@Single
class GetCelestialBodiesUseCase(private val repository: CelestialBodiesRepository) {

    suspend operator fun invoke(): Result<List<CelestialBody?>> {
        return repository.getCelestialBodies()
    }
}