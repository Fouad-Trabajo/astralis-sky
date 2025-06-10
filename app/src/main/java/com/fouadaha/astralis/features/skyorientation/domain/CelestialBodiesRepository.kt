package com.fouadaha.astralis.features.skyorientation.domain

import com.fouadaha.astralis.core.domain.model.CelestialBody

interface CelestialBodiesRepository {

    suspend fun getCelestialBodies(): Result<List<CelestialBody>>
}