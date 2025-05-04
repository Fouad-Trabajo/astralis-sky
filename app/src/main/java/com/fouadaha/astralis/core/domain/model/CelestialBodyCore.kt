package com.fouadaha.astralis.core.domain.model


data class CelestialBodyCore(
    val id: String,
    val name: String,
    val description: String?,
    val characteristics: Characteristics?,
    val orbitalParameters: OrbitalParameters?,
    val imageUrl: String?,
)

data class Characteristics(
    val id: String,
    val mass: String,
    val celestialBodyType: CelestialBodyType,
    val radius: String,
    val density: String,
    val temperature: String,
    val gravity: String,
)

data class OrbitalParameters(
    val semiMajorAxis: Float,
    val eccentricity: Float,
    val inclination: Float,
    val ascendingNodeLongitude: Float,
    val argumentOfPeriapsis: Float,
    val meanAnomaly: Float
)

enum class CelestialBodyType {
    PLANET, DWARF_PLANET, STAR, ASTEROID, BLACK_HOLE,
    SATELLITE, ARTIFICIAL_SATELLITE, GALAXY, UNKNOWN_BODY
}