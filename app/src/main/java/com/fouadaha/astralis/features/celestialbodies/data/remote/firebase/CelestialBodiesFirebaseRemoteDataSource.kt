package com.fouadaha.astralis.features.celestialbodies.data.remote.firebase

import com.fouadaha.astralis.core.domain.model.CelestialBody
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.koin.core.annotation.Single

@Single
class CelestialBodiesFirebaseRemoteDataSource(private val fireStore: FirebaseFirestore) {

    suspend fun getCelestialBodies(): Result<List<CelestialBody>> {
        val celestialBodies = fireStore.collection("bodies")
            .get()
            .await()
            .documents
            .mapNotNull {
                it.toObject(CelestialBodiesFirebaseModel::class.java)?.toDomain()
            }
        return Result.success(celestialBodies)
    }
}