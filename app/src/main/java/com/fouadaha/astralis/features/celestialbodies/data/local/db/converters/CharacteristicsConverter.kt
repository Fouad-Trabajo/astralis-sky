package com.fouadaha.astralis.features.celestialbodies.data.local.db.converters

import androidx.room.TypeConverter
import com.fouadaha.astralis.features.celestialbodies.domain.Characteristics
import com.google.gson.Gson

class CharacteristicsConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromCharacteristicsToString(characteristics: Characteristics): String {
        return gson.toJson(characteristics, Characteristics::class.java)
    }

    @TypeConverter
    fun fromStringToCharacteristics(characteristics: String): Characteristics {
        return gson.fromJson(characteristics, Characteristics::class.java)
    }
}