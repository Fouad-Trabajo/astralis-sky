package com.fouadaha.astralis.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Entity_example")
class EntityExample(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "createdAt") val date: Date
)