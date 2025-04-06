package com.fouadaha.astralis.features.celestialbodies.data.remote.firebase

import com.google.firebase.firestore.PropertyName

data class CelestialBodiesFirebaseModel(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("characteristics") val characteristics:
    CharacteristicsFirebaseModel = CharacteristicsFirebaseModel(),
    @PropertyName("imageUrl") val imageUrl: String = ""
)

data class CharacteristicsFirebaseModel(
    @PropertyName("id") val id: String = "",
    @PropertyName("mass") val mass: String = "",
    @PropertyName("celestialBodyType") val celestialBodyType: String = "",
    @PropertyName("radius") val radius: String = "",
    @PropertyName("density") val density: String = "",
    @PropertyName("temperature") val temperature: String = "",
    @PropertyName("gravity") val gravity: String = ""
)

