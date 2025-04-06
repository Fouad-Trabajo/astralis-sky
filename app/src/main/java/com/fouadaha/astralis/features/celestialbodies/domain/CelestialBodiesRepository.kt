package com.fouadaha.astralis.features.celestialbodies.domain

interface CelestialBodiesRepository {

    suspend fun getCelestialBodies(): Result<List<CelestialBody>>
    suspend fun getCelestialBody(id: String): Result<CelestialBody?>
}