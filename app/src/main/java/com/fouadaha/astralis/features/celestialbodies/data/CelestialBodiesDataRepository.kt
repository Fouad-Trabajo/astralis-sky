package com.fouadaha.astralis.features.celestialbodies.data

import com.fouadaha.astralis.features.celestialbodies.data.local.db.CelestialBodiesRoomLocalDataSource
import com.fouadaha.astralis.features.celestialbodies.data.remote.firebase.CelestialBodiesFirebaseRemoteDataSource
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBodiesRepository
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBody
import org.koin.core.annotation.Single

@Single
class CelestialBodiesDataRepository(
    private val localDataSource: CelestialBodiesRoomLocalDataSource,
    private val remoteDataSource: CelestialBodiesFirebaseRemoteDataSource
) : CelestialBodiesRepository {
    override suspend fun getCelestialBodies(): Result<List<CelestialBody>> {
        val roomData = localDataSource.getAll()
        return if (roomData.isFailure) {
            remoteDataSource.getCelestialBodies().onSuccess {
                localDataSource.saveAll(it)
            }
        } else {
            Result.success(roomData.getOrNull() ?: emptyList())
        }
    }

    override suspend fun getCelestialBody(id: String): Result<CelestialBody?> {
        val roomData = localDataSource.getById(id)
        return Result.success(roomData.getOrNull())
    }
}
