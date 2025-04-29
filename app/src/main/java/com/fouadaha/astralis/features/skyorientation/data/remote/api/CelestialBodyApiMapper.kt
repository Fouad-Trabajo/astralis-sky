package com.fouadaha.astralis.features.skyorientation.data.remote.api

import com.fouadaha.astralis.features.skyorientation.domain.CelestialBody
import com.fouadaha.astralis.features.skyorientation.domain.OrbitalParameters

fun CelestialBodyApiModel.toDomain(): CelestialBody {
    return CelestialBody(
        id = this.id, name = this.name, isPlanet = this.isPlanet,
        orbitalParameters = OrbitalParameters(
            semiMajorAxis = this.semiMajorAxis,
            eccentricity = this.eccentricity,
            inclination = this.inclination,
            ascendingNodeLongitude = this.ascendingNodeLongitude,
            argumentOfPeriapsis = this.argumentOfPeriapsis,
            meanAnomaly = this.meanAnomaly
        )
    )
}