package com.fouadaha.astralis.features.celestialobodies.data

import com.fouadaha.astralis.features.celestialobodies.data.local.db.CelestialBodiesRoomLocalDataSource
import com.fouadaha.astralis.features.celestialobodies.data.remote.firebase.CelestialBodiesFirebaseRemoteDataSource
import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBodiesRepository
import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBody
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
