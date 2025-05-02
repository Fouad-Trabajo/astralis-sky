package com.fouadaha.astralis.features.skyorientation.data.local

import android.content.Context

import com.fouadaha.astralis.R
import com.fouadaha.astralis.features.skyorientation.domain.CelestialBody
import com.google.gson.Gson
import org.koin.core.annotation.Single

@Single
class CelestialBodiesXmlLocalDataSource(private val context: Context) {

    private val gson = Gson()

    private val sharedPref =
        context.getSharedPreferences(
            context.getString(R.string.bodies_file_xml),
            Context.MODE_PRIVATE
        )

    fun saveLocalBodies(celestialBodies: List<CelestialBody>) {
        val editor = sharedPref.edit()
        celestialBodies.forEach { celestialBody ->
            editor.putString(celestialBody.id, gson.toJson(celestialBody))
            editor.apply()
        }
    }

    fun getLocalBodies(): Result<List<CelestialBody>> {
        return try {
            val bodies = mutableListOf<CelestialBody>()
            val map = sharedPref.all
            map.values.forEach { jsonBody ->
                val body = gson.fromJson(jsonBody.toString(), CelestialBody::class.java)
                bodies.add(body)
            }
            Result.success(bodies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}