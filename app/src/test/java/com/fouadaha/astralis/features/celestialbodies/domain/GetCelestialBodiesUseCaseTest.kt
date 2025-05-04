package com.fouadaha.astralis.features.celestialbodies.domain

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue


class GetCelestialBodiesUseCaseTest {

    @RelaxedMockK
    private lateinit var celestialBodiesRepository: CelestialBodiesRepository

    private lateinit var getCelestialBodiesUseCase: GetCelestialBodiesUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        getCelestialBodiesUseCase = GetCelestialBodiesUseCase(celestialBodiesRepository)
    }

    @Test
    fun `cuando el repositorio devuelve una lista vacia`() = runBlocking {
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
    fun `cuando el repositorio devuelve una lista de cuerpos celestes`() = runBlocking {
        // Given
        val characteristics = Characteristics(
            id = "1",
            mass = "",
            celestialBodyType = CelestialBodyType.PLANET, // Ajuste aquí
            radius = "",
            density = "",
            temperature = "",
            gravity = ""
        )

        val expectedList = listOf(
            CelestialBody(
                id = "1",
                name = "Mars",
                description = "",
                characteristics = characteristics,
                imageUrl = ""
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
    fun `cuando el repositorio devuelve un error`() = runBlocking {
        // Given
        val exception = RuntimeException("Error al cargar cuerpos celestes")
        coEvery { celestialBodiesRepository.getCelestialBodies() } returns Result.failure(exception)

        // When
        val result = getCelestialBodiesUseCase()

        // Then
        coVerify(exactly = 1) { celestialBodiesRepository.getCelestialBodies() }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

}