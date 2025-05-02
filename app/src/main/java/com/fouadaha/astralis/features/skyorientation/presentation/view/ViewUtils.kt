package com.fouadaha.astralis.features.skyorientation.presentation.view

import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

const val PERSPECTIVE = 800f
const val SPEED_ORBITAL = 0.8f
private var startTime = System.nanoTime()

fun Float.toRadians() = Math.toRadians(this.toDouble()).toFloat()

// Función que permita crear objetos que vamos a pintar con los atributos que queramos
private fun createPaint(color: Int, strokeWidth: Float = 0f, textSize: Float = 0f): Paint =
    Paint().apply {
        isAntiAlias = true
        this.color = color
        this.strokeWidth = strokeWidth
        this.textSize = textSize
    }

val paintStar = createPaint(color = Color.WHITE)
val paintOrbit = createPaint(color = Color.CYAN, strokeWidth = 4f)
val paintPlanet = createPaint(color = Color.RED)
val paintText = createPaint(color = Color.WHITE, textSize = 28f)


//Sirve para calcular la posición del planeta en la orbita para dibujr el puntito
fun keplerFunction(meanAnomaly: Float, eccentricity: Float): Float {
    // m=E−e⋅sin(E)
    var e = meanAnomaly
    for (i in 0..4) {
        e -= (e - eccentricity * sin(e) - meanAnomaly) / (1 - eccentricity * cos(e))
    }
    return e
}

//Tiempo transcurrido desde el inicio del programa
fun getElapsedTimeInSeconds(): Float {
    val now = System.nanoTime()
    return (now - startTime) / 1000000000f
}