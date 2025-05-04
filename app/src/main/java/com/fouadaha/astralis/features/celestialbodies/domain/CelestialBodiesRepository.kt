package com.fouadaha.astralis.features.celestialbodies.domain

import com.fouadaha.astralis.core.domain.model.CelestialBodyCore

interface CelestialBodiesRepository {

    suspend fun getCelestialBodies(): Result<List<CelestialBodyCore>>
    suspend fun getCelestialBody(id: String): Result<CelestialBodyCore?>
}