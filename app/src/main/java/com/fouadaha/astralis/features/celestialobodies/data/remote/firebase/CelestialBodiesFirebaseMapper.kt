package com.fouadaha.astralis.features.celestialobodies.data.remote.firebase

import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBody
import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBodyType
import com.fouadaha.astralis.features.celestialobodies.domain.Characteristics

fun CelestialBodiesFirebaseModel.toDomain(): CelestialBody {
    return CelestialBody(
        this.id,
        this.name,
        this.description,
        this.characteristics.toDomain(),
        this.imageUrl
    )
}

fun CharacteristicsFirebaseModel.toDomain(): Characteristics {
    return Characteristics(
        this.id,
        this.mass,
        celestialBodyType = CelestialBodyType.entries.find {
            it.name == this.celestialBodyType
        } ?: CelestialBodyType.UNKNOWN_BODY,
        this.radius,
        this.density,
        this.temperature,
        this.gravity
    )
}