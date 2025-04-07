package com.fouadaha.astralis.features.celestialbodies.data.local.db

import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBody
import java.util.Date

fun CelestialBody.toEntity(): CelestialBodyEntity =
    CelestialBodyEntity(
        this.id,
        this.name,
        this.description,
        this.characteristics,
        this.imageUrl,
        date = Date()
    )

fun CelestialBodyEntity.toDomain(): CelestialBody =
    CelestialBody(
        this.id,
        this.name,
        this.description,
        this.characteristics,
        this.imageUrl
    )