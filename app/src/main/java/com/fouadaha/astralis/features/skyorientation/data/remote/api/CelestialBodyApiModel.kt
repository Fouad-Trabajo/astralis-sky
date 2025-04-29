package com.fouadaha.astralis.features.skyorientation.data.remote.api

import com.google.gson.annotations.SerializedName

data class BodiesResponse(
    @SerializedName("bodies") val bodies: List<CelestialBodyApiModel>
)

data class CelestialBodyApiModel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("isPlanet") val isPlanet: Boolean,
    @SerializedName("semimajorAxis") val semiMajorAxis: Float,
    @SerializedName("eccentricity") val eccentricity: Float,
    @SerializedName("inclination") val inclination: Float,
    @SerializedName("longAscNode") val ascendingNodeLongitude: Float,
    @SerializedName("argPeriapsis") val argumentOfPeriapsis: Float,
    @SerializedName("mainAnomaly") val meanAnomaly: Float
)
