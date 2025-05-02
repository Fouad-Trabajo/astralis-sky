package com.fouadaha.astralis.features.skyorientation.presentation.view

import com.fouadaha.astralis.features.skyorientation.domain.DeviceOrientation
import com.fouadaha.astralis.features.skyorientation.domain.Point3D
import kotlin.math.cos
import kotlin.math.sin

fun rotate3D(x: Float, y: Float, z: Float, orientation: DeviceOrientation): Point3D {
    val pitchRadians = orientation.pitch.toRadians()
    val rollRadians = orientation.roll.toRadians()
    val yawRadians = orientation.azimuth.toRadians()

    // Rotación sobre el eje X (pitch)
    val rotateXAboutPitch = x
    val rotateYAboutPitch = y * cos(pitchRadians) - z * sin(pitchRadians)
    val rotateZAboutPitch = y * sin(pitchRadians) + z * cos(pitchRadians)

    // Rotación sobre el eje Y (roll)
    val rotateXAboutRoll =
        rotateXAboutPitch * cos(rollRadians) + rotateZAboutPitch * sin(rollRadians)
    val rotateZAboutRoll =
        -rotateXAboutPitch * sin(rollRadians) + rotateZAboutPitch * cos(rollRadians)

    // Rotación sobre el eje Z (yaw/azimuth)
    val rotateXAboutYaw =
        rotateXAboutRoll * cos(yawRadians) - rotateYAboutPitch * sin(yawRadians)
    val rotateYAboutYaw =
        rotateXAboutRoll * sin(yawRadians) + rotateYAboutPitch * cos(yawRadians)

    return Point3D(rotateXAboutYaw, rotateYAboutYaw, rotateZAboutRoll)
}