package com.fouadaha.astralis.features.celestialbodies.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fouadaha.astralis.features.celestialbodies.domain.Characteristics
import java.util.Date

const val CELESTIAL_BODY_TABLE = "celestial_body"
const val CELESTIAL_BODY_ID = "celestial_body_id"

@Entity(tableName = CELESTIAL_BODY_TABLE)
class CelestialBodyEntity(
    @PrimaryKey @ColumnInfo(name = CELESTIAL_BODY_ID) val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "characteristics") val characteristics: Characteristics,
    @ColumnInfo(name = "imageUrl") val imageUrl: String,
    @ColumnInfo(name = "createdAt") val date: Date
)


