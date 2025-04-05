package com.fouadaha.astralis.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fouadaha.astralis.core.data.local.converters.DateConverter

@Database(entities = [EntityExample::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AstralisDataBase : RoomDatabase() {

}