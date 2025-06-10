package com.fouadaha.astralis.features.celestialbodies.domain

import com.fouadaha.astralis.core.domain.model.CelestialBody
import org.koin.core.annotation.Single

@Single
class GetCelestialBodiesUseCase(private val celestialBodiesRepository: CelestialBodiesRepository) {

    suspend operator fun invoke(): Result<List<CelestialBody>> {
        return celestialBodiesRepository.getCelestialBodies()
    }
}