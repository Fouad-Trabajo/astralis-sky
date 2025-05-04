package com.fouadaha.astralis.features.celestialbodies.data.local.db

import com.fouadaha.astralis.core.domain.model.CelestialBodyCore
import java.util.Date

fun CelestialBodyCore.toEntity(): CelestialBodyEntity =
    CelestialBodyEntity(
        id = this.id,
        name = this.name,
        description = this.description ?: "",
        characteristics = this.characteristics!!,
        imageUrl = this.imageUrl ?: "",
        date = Date()
    )

fun CelestialBodyEntity.toDomain(): CelestialBodyCore =
    CelestialBodyCore(
        id = this.id,
        name = this.name,
        description = this.description,
        characteristics = this.characteristics,
        orbitalParameters = null,
        imageUrl = this.imageUrl
    )