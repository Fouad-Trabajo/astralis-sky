package com.fouadaha.astralis.features.skyorientation.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.fouadaha.astralis.features.skyorientation.domain.CelestialBody
import com.fouadaha.astralis.features.skyorientation.domain.DeviceOrientation
import com.fouadaha.astralis.features.skyorientation.domain.Point3D
import com.fouadaha.astralis.features.skyorientation.presentation.view.PERSPECTIVE
import com.fouadaha.astralis.features.skyorientation.presentation.view.SPEED_ORBITAL
import com.fouadaha.astralis.features.skyorientation.presentation.view.getElapsedTimeInSeconds
import com.fouadaha.astralis.features.skyorientation.presentation.view.keplerFunction
import com.fouadaha.astralis.features.skyorientation.presentation.view.paintOrbit
import com.fouadaha.astralis.features.skyorientation.presentation.view.paintPlanet
import com.fouadaha.astralis.features.skyorientation.presentation.view.paintStar
import com.fouadaha.astralis.features.skyorientation.presentation.view.paintText
import com.fouadaha.astralis.features.skyorientation.presentation.view.rotate3D
import com.fouadaha.astralis.features.skyorientation.presentation.view.toRadians

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class OrbitView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val random = Random
    private var celestialBodies: List<CelestialBody> = emptyList()
    private val stars = mutableListOf<Triple<Float, Float, Float>>()
    private var orientation: DeviceOrientation = DeviceOrientation(0f, 0f, 0f)
    private var globalScaleFactor = 1f


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK) //Background color
        drawStars(canvas) //Paint stars

        // In center of the screen
        val centerX = width / 2f
        val centerY = height / 2f

        val maxDistance = celestialBodies
            .filter { it.isPlanet }
            .maxOfOrNull { it.celestialBody.orbitalParameters!!.semiMajorAxis }?.div(1e8f) ?: 1f
        val scaleFactor = (width.coerceAtMost(height) / 2f) / maxDistance * 0.9f

        val scale = scaleFactor * globalScaleFactor

        celestialBodies.filter { it.isPlanet }.forEach { body ->
            body.celestialBody.orbitalParameters!!.apply {
                val majorAxis = semiMajorAxis / 1e8f
                val eccentricity = eccentricity.toRadians()
                val inclination = inclination.toRadians()
                val node = ascendingNodeLongitude.toRadians()
                val secondsElapsed = getElapsedTimeInSeconds()
                val anomaly = meanAnomaly.toRadians() + secondsElapsed * SPEED_ORBITAL

                val eccentricAnomaly = keplerFunction(anomaly, eccentricity)
                val radius = majorAxis * (1 - eccentricity * cos(eccentricAnomaly))

                val x = radius * (cos(node) * cos(eccentricAnomaly - eccentricity) - sin(node)
                        * sin(eccentricAnomaly - eccentricity) * cos(inclination))
                val y = radius * (sin(node) * cos(eccentricAnomaly - eccentricity) + cos(node)
                        * sin(eccentricAnomaly - eccentricity) * cos(inclination))
                val z = radius * sin(eccentricAnomaly - eccentricity) * sin(inclination)

                val rotated = rotate3D(x, y, z, orientation)

                val projectionX =
                    (rotated.x * PERSPECTIVE / (PERSPECTIVE + rotated.z)) * scale + centerX
                val projectionY =
                    (rotated.y * PERSPECTIVE / (PERSPECTIVE + rotated.z)) * scale + centerY

                drawOrbit(
                    canvas, majorAxis, eccentricity,
                    inclination, node, centerX, centerY, scale
                )
                canvas.drawCircle(projectionX, projectionY, 10f, paintPlanet)
                canvas.drawText(
                    body.celestialBody.name,
                    projectionX + 12f,
                    projectionY - 12f,
                    paintText
                )
            }
        }
        postInvalidateOnAnimation() //redibujar automáticamente
    }

    private fun drawOrbit(
        canvas: Canvas, majorAxis: Float, eccentricity: Float, inclination: Float, node: Float,
        centerX: Float, centerY: Float, scale: Float
    ) {
        val orbitPoints = mutableListOf<Point3D>()
        for (alpha in 0..360 step 5) {
            val angleInRadians = alpha.toFloat().toRadians()
            val radius = majorAxis * (1 - eccentricity * eccentricity) /
                    (1 + eccentricity * cos(angleInRadians))

            val x = radius * (cos(node) * cos(angleInRadians - eccentricity)
                    - sin(node) * sin(angleInRadians - eccentricity) * cos(inclination))
            val y = radius * (sin(node) * cos(angleInRadians - eccentricity)
                    + cos(node) * sin(angleInRadians - eccentricity) * cos(inclination))
            val z = radius * sin(angleInRadians - eccentricity) * sin(inclination)

            val rotated = rotate3D(x, y, z, orientation)
            val projectionX = (rotated.x * PERSPECTIVE /
                    (PERSPECTIVE + rotated.z)) * scale + centerX
            val projectionY = (rotated.y * PERSPECTIVE /
                    (PERSPECTIVE + rotated.z)) * scale + centerY
            orbitPoints.add(Point3D(projectionX, projectionY, 0f))
        }

        for (i in 0 until orbitPoints.size - 1) {
            val p1 = orbitPoints[i]
            val p2 = orbitPoints[i + 1]
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintOrbit)
        }
    }


    fun updateBodies(bodies: List<CelestialBody?>) {
        celestialBodies = bodies as List<CelestialBody>
        invalidate()
    }

    var deviceOrientation: DeviceOrientation
        get() = orientation
        set(value) {
            orientation = value
            invalidate()
        }


    private fun generateStars() {
        stars.clear()
        val starCount = 200
        for (i in 0 until starCount) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            val size = random.nextFloat() * 4.5f + 0.5f
            stars.add(Triple(x, y, size))
        }
    }

    private fun drawStars(canvas: Canvas) {
        for ((x, y, size) in stars) {
            canvas.drawCircle(x, y, size, paintStar)
        }
    }

    private val scaleGestureDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                globalScaleFactor *= detector.scaleFactor
                globalScaleFactor = globalScaleFactor.coerceIn(1.0f, 50.0f) // Limit zoom
                paintText.textSize = (32f * globalScaleFactor).coerceIn(12f, 50f) //Texto escalado
                invalidate()
                return true
            }
        })

    //Detectar gestos y movimientos en la pantalla
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        return true
    }

    //Reacciona a cambios en el tamaño de la vista, por ejemplo al crearla
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateStars()
    }
}