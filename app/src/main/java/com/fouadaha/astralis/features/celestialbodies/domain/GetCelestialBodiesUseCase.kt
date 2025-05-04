package com.fouadaha.astralis.features.celestialbodies.domain

import com.fouadaha.astralis.core.domain.model.CelestialBodyCore
import org.koin.core.annotation.Single

@Single
class GetCelestialBodiesUseCase(private val celestialBodiesRepository: CelestialBodiesRepository) {

    suspend operator fun invoke(): Result<List<CelestialBodyCore>> {
        return celestialBodiesRepository.getCelestialBodies()
    }
}