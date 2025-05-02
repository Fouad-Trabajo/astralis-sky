package com.fouadaha.astralis.features.skyorientation.domain

data class CelestialBody(
    val id: String,
    val name: String,
    val orbitalParameters: OrbitalParameters,
    val isPlanet: Boolean
)

data class OrbitalParameters(
    val semiMajorAxis: Float,
    val eccentricity: Float,
    val inclination: Float,
    val ascendingNodeLongitude: Float,
    val argumentOfPeriapsis: Float,
    val meanAnomaly: Float
)

data class DeviceOrientation(
    val azimuth: Float,  // Eje Z: dirección norte-sur, este-oeste
    val pitch: Float,    // Eje X: inclinación hacia arriba/abajo
    val roll: Float      // Eje Y: inclinación hacia izquierda/derecha
)

data class Point3D(val x: Float, val y: Float, val z: Float)