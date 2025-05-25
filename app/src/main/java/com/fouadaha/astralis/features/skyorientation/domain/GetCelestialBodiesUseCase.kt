package com.fouadaha.astralis.features.skyorientation.domain

import com.fouadaha.astralis.core.domain.model.CelestialBody
import org.koin.core.annotation.Single

@Single
class GetCelestialBodiesUseCase(private val repository: CelestialBodiesRepository) {

    suspend operator fun invoke(): Result<List<CelestialBody>> {
        return repository.getCelestialBodies()
    }
}