package com.fouadaha.astralis.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fouadaha.astralis.core.data.local.converters.DateConverter
import com.fouadaha.astralis.features.celestialobodies.data.local.db.CelestialBodiesDao
import com.fouadaha.astralis.features.celestialobodies.data.local.db.CelestialBodyEntity
import com.fouadaha.astralis.features.celestialobodies.data.local.db.converters.CharacteristicsConverter

@Database(entities = [CelestialBodyEntity::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class, CharacteristicsConverter::class)
abstract class AstralisDataBase : RoomDatabase() {
    abstract fun celestialBodiesDao(): CelestialBodiesDao
}