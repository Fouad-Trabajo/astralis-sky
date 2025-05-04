package com.fouadaha.astralis.features.skyorientation.domain

import com.fouadaha.astralis.core.domain.model.CelestialBodyCore

data class CelestialBody(
    val celestialBody: CelestialBodyCore,
    val isPlanet: Boolean
)

data class DeviceOrientation(
    val azimuth: Float,  // Eje Z: dirección norte-sur, este-oeste
    val pitch: Float,    // Eje X: inclinación hacia arriba/abajo
    val roll: Float      // Eje Y: inclinación hacia izquierda/derecha
)

data class Point3D(val x: Float, val y: Float, val z: Float)