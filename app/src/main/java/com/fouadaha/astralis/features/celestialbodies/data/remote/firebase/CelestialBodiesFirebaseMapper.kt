package com.fouadaha.astralis.features.celestialbodies.data.remote.firebase

import com.fouadaha.astralis.core.domain.model.CelestialBodyCore
import com.fouadaha.astralis.core.domain.model.CelestialBodyType
import com.fouadaha.astralis.core.domain.model.Characteristics


fun CelestialBodiesFirebaseModel.toDomain(): CelestialBodyCore {
    return CelestialBodyCore(
        id = this.id,
        name = this.name,
        description = this.description,
        characteristics = this.characteristics.toDomain(),
        orbitalParameters = null,
        imageUrl = this.imageUrl
    )
}

fun CharacteristicsFirebaseModel.toDomain(): Characteristics {
    return Characteristics(
        id = this.id,
        mass = this.mass,
        celestialBodyType = CelestialBodyType.entries.find {
            it.name == this.celestialBodyType
        } ?: CelestialBodyType.UNKNOWN_BODY,
        radius = this.radius,
        density = this.density,
        temperature = this.temperature,
        gravity = this.gravity
    )
}