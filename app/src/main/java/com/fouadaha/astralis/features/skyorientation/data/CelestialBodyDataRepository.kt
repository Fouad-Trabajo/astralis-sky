package com.fouadaha.astralis.features.skyorientation.data

import com.fouadaha.astralis.features.skyorientation.data.local.CelestialBodiesXmlLocalDataSource
import com.fouadaha.astralis.features.skyorientation.data.remote.api.CelestialBodyApiDataSource
import com.fouadaha.astralis.features.skyorientation.domain.CelestialBodiesRepository
import com.fouadaha.astralis.features.skyorientation.domain.CelestialBody
import org.koin.core.annotation.Single

@Single
class CelestialBodyDataRepository(
    private val apiDataSource: CelestialBodyApiDataSource,
    private val localDataSource: CelestialBodiesXmlLocalDataSource
) : CelestialBodiesRepository {

    override suspend fun getCelestialBodies(): Result<List<CelestialBody>> {
        val localBodies = localDataSource.getLocalBodies().getOrNull() ?: emptyList()
        return if (localBodies.isEmpty()) {
            apiDataSource.getAllBodies().onSuccess {
                localDataSource.saveLocalBodies(it)
            }
        } else {
            Result.success(localBodies)
        }
    }
}