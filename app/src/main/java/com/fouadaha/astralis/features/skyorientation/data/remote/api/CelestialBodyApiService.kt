package com.fouadaha.astralis.features.skyorientation.data.remote.api

import retrofit2.Response
import retrofit2.http.GET

interface CelestialBodyApiService {

    @GET("bodies")
    suspend fun getAllBodies(): Response<BodiesResponse>
}