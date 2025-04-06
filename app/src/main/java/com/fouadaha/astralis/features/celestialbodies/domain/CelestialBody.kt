package com.fouadaha.astralis.features.celestialbodies.domain

data class CelestialBody(
    val id: String,
    val name: String,
    val description: String,
    val characteristics: Characteristics,
    val imageUrl: String
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

enum class CelestialBodyType {
    PLANET, DWARF_PLANET, STAR, ASTEROID, BLACK_HOLE,
    SATELLITE, ARTIFICIAL_SATELLITE, GALAXY, UNKNOWN_BODY
}