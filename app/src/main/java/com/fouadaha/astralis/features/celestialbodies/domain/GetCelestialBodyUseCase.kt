package com.fouadaha.astralis.features.celestialbodies.domain

import com.fouadaha.astralis.core.domain.model.CelestialBodyCore
import org.koin.core.annotation.Single

@Single
class GetCelestialBodyUseCase(private val celestialBoyRepository: CelestialBodiesRepository) {

    suspend operator fun invoke(id: String): Result<CelestialBodyCore?> {
        return celestialBoyRepository.getCelestialBody(id)
    }
}