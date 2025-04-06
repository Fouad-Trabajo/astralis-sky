package com.fouadaha.astralis.features.celestialbodies.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CelestialBodiesDao {

    //Métodos de busqueda
    @Query("SELECT * FROM $CELESTIAL_BODY_TABLE")
    suspend fun getAll(): List<CelestialBodyEntity>

    @Query("SELECT * FROM $CELESTIAL_BODY_TABLE WHERE $CELESTIAL_BODY_ID = :id")
    suspend fun getById(id: String): CelestialBodyEntity

    //Métodos de conveniencia
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(vararg celestialBody: CelestialBodyEntity)
}