package com.fouadaha.astralis.features.celestialbodies.domain

import org.koin.core.annotation.Single

@Single
class GetCelestialBodyUseCase(private val celestialBoyRepository: CelestialBodiesRepository) {

    suspend operator fun invoke(id: String): Result<CelestialBody?> {
        return celestialBoyRepository.getCelestialBody(id)
    }
}