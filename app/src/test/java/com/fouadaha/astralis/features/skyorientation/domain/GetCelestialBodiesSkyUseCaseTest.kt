package com.fouadaha.astralis.features.skyorientation.domain

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue


class GetCelestialBodiesSkyUseCaseTest {

    @RelaxedMockK
    private lateinit var celestialBodiesRepository: CelestialBodiesRepository

    private lateinit var getCelestialBodiesUseCase: GetCelestialBodiesUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        getCelestialBodiesUseCase = GetCelestialBodiesUseCase(celestialBodiesRepository)
    }

    @Test
    fun `cuando el repositorio devuelve una lista null`() = runBlocking {
        // Given
        coEvery { celestialBodiesRepository.getCelestialBodies() } returns Result.success(emptyList())

        // When
        val result = getCelestialBodiesUseCase()

        // Then
        coVerify(exactly = 1) { celestialBodiesRepository.getCelestialBodies() }
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `cuando el repositorio devuelve una lista llena de cuerpos`() = runBlocking {
        // Given
        val orbitalParams = OrbitalParameters(
            semiMajorAxis = 1.0f,
            eccentricity = 0.0167f,
            inclination = 0.0f,
            ascendingNodeLongitude = 0.0f,
            argumentOfPeriapsis = 102.9f,
            meanAnomaly = 0.0f
        )

        val expectedList = listOf(
            CelestialBody(
                id = "earth",
                name = "Earth",
                orbitalParameters = orbitalParams,
                isPlanet = true
            )
        )

        coEvery { celestialBodiesRepository.getCelestialBodies() } returns Result.success(
            expectedList
        )

        // When
        val result = getCelestialBodiesUseCase()

        // Then
        coVerify(exactly = 1) { celestialBodiesRepository.getCelestialBodies() }
        assertTrue(result.isSuccess)
        assertEquals(expectedList, result.getOrNull())
    }

    @Test
    fun `cuando el repositorio devuelve un fail`() = runBlocking {
        // Given
        val exception = RuntimeException("Fallo al obtener cuerpos celestes")
        coEvery { celestialBodiesRepository.getCelestialBodies() } returns Result.failure(exception)

        // When
        val result = getCelestialBodiesUseCase()

        // Then
        coVerify(exactly = 1) { celestialBodiesRepository.getCelestialBodies() }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}