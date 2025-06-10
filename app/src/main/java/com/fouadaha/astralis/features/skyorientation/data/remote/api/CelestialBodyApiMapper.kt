package com.fouadaha.astralis.features.skyorientation.data.remote.api

import com.fouadaha.astralis.core.domain.model.CelestialBody
import com.fouadaha.astralis.core.domain.model.OrbitalParameters

fun CelestialBodyApiModel.toDomain(): CelestialBody {
    return CelestialBody(
        id = id, name = name, isPlanet = isPlanet,
        description = null, characteristics = null, imageUrl = null,
        orbitalParameters = OrbitalParameters(
            semiMajorAxis = semiMajorAxis,
            eccentricity = eccentricity,
            inclination = inclination,
            ascendingNodeLongitude = ascendingNodeLongitude,
            argumentOfPeriapsis = argumentOfPeriapsis,
            meanAnomaly = meanAnomaly
        )
    )
}