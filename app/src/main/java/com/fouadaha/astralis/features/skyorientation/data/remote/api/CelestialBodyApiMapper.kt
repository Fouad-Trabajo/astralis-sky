package com.fouadaha.astralis.features.skyorientation.data.remote.api

import com.fouadaha.astralis.core.domain.model.CelestialBodyCore
import com.fouadaha.astralis.core.domain.model.OrbitalParameters
import com.fouadaha.astralis.features.skyorientation.domain.CelestialBody

fun CelestialBodyApiModel.toDomain(): CelestialBody? {
    if (
        semiMajorAxis == 0f ||
        eccentricity == 0f ||
        inclination == 0f ||
        ascendingNodeLongitude == 0f ||
        argumentOfPeriapsis == 0f ||
        meanAnomaly == 0f
    ) {
        return null
    }
    return CelestialBody(
        celestialBody = CelestialBodyCore(
            id = this.id, name = this.name,
            description = null, characteristics = null, imageUrl = null,
            orbitalParameters = OrbitalParameters(
                semiMajorAxis = this.semiMajorAxis,
                eccentricity = this.eccentricity,
                inclination = this.inclination,
                ascendingNodeLongitude = this.ascendingNodeLongitude,
                argumentOfPeriapsis = this.argumentOfPeriapsis,
                meanAnomaly = this.meanAnomaly
            )
        ), isPlanet = this.isPlanet
    )
}