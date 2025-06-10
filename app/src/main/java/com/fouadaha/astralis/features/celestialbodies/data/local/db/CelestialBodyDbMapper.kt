package com.fouadaha.astralis.features.celestialbodies.data.local.db

import com.fouadaha.astralis.core.domain.model.CelestialBody
import java.util.Date

fun CelestialBody.toEntity(): CelestialBodyEntity {
    return CelestialBodyEntity(
        id = id,
        name = name,
        description = description,
        characteristics = characteristics,
        imageUrl = imageUrl,
        date = Date()
    )
}

fun CelestialBodyEntity.toDomain(): CelestialBody {
    return CelestialBody(
        id = id,
        name = name,
        description = description,
        characteristics = characteristics,
        imageUrl = imageUrl,
        orbitalParameters = null,
        isPlanet = null
    )
}