package com.fouadaha.astralis.features.celestialbodies.data.remote.firebase

import com.fouadaha.astralis.core.domain.model.CelestialBody
import com.fouadaha.astralis.core.domain.model.CelestialBodyType
import com.fouadaha.astralis.core.domain.model.Characteristics


fun CelestialBodiesFirebaseModel.toDomain(): CelestialBody {
    return CelestialBody(
        id = id,
        name = name,
        description = description,
        characteristics = characteristics.toDomain(),
        imageUrl = imageUrl,
        orbitalParameters = null,
        isPlanet = null
    )
}

fun CharacteristicsFirebaseModel.toDomain(): Characteristics {
    return Characteristics(
        id = id,
        mass = mass,
        celestialBodyType = CelestialBodyType.entries.find { //Obtener lista de enum y filtrar
            it.name == this.celestialBodyType
        } ?: CelestialBodyType.UNKNOWN_BODY,
        radius = radius,
        density = density,
        temperature = temperature,
        gravity = gravity
    )
}