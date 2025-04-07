package com.fouadaha.astralis.features.celestialbodies.data.local.db

import com.fouadaha.astralis.core.domain.ErrorApp
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBody
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
        dao.saveAll(*entities.toTypedArray())
    }

    suspend fun getById(id: String): Result<CelestialBody> {
        val entity = dao.getById(id)
        return Result.success(entity.toDomain())
    }
}