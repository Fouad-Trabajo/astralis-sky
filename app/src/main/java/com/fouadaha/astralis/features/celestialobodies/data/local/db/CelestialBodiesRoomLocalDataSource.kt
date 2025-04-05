package com.fouadaha.astralis.features.celestialobodies.data.local.db

import android.util.Log
import com.fouadaha.astralis.core.domain.ErrorApp
import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBody
import org.koin.core.annotation.Single

const val TTL = 30000L

@Single
class CelestialBodiesRoomLocalDataSource(private val dao: CelestialBodiesDao) {

    suspend fun getAll(): Result<List<CelestialBody>> {
        val entities = dao.getAll()

        val validEntity = entities.filter { entity ->
            val timeDifference = System.currentTimeMillis() - entity.date.time
            timeDifference <= TTL
        }
        return if (validEntity.isEmpty()) {
            Result.failure(ErrorApp.DataExpiredError)
        } else {
            Result.success(entities.map { it.toDomain() })
        }
    }

    suspend fun saveAll(celestialBody: List<CelestialBody>) {
        val entities = celestialBody.map { it.toEntity() }
        Log.d("CelestialBodiesRoomLocalDataSource", "Entities: $entities")
        dao.saveAll(*entities.toTypedArray())
        Log.d("CelestialBodiesRoomLocalDataSource", "Entities: $entities")
    }

    suspend fun getById(id: String): Result<CelestialBody> {
        val entity = dao.getById(id)
        return Result.success(entity.toDomain())
    }
}