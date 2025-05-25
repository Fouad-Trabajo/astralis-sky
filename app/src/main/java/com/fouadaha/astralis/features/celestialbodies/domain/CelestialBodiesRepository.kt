package com.fouadaha.astralis.features.celestialbodies.domain

import com.fouadaha.astralis.core.domain.model.CelestialBody

interface CelestialBodiesRepository {

    suspend fun getCelestialBodies(): Result<List<CelestialBody>>
    suspend fun getCelestialBody(id: String): Result<CelestialBody?>
}