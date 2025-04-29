package com.fouadaha.astralis.features.skyorientation.domain

interface CelestialBodiesRepository {

    suspend fun getCelestialBodies(): Result<List<CelestialBody>>
}