package com.fouadaha.astralis.features.celestialbodies.domain


import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue


class GetCelestialBodyUseCaseTest {

    @RelaxedMockK
    private lateinit var celestialBodiesRepository: CelestialBodiesRepository

    private lateinit var getCelestialBodyUseCase: GetCelestialBodyUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        getCelestialBodyUseCase = GetCelestialBodyUseCase(celestialBodiesRepository)
    }

    @Test
    fun `cuando el cuerpo celeste existe, devuelve el cuerpo esperado`() = runBlocking {
        // Given
        val id = "mars"
        val characteristics = Characteristics(
            id = "1",
            mass = "6.39 × 10^23 kg",
            radius = "3389.5 km",
            density = "3.93 g/cm³",
            temperature = "-63°C",
            gravity = "3.721 m/s²",
            celestialBodyType = CelestialBodyType.PLANET
        )
        val expectedBody = CelestialBody(
            id = id,
            name = "Mars",
            description = "Red planet",
            characteristics = characteristics,
            imageUrl = ""
        )
        coEvery { celestialBodiesRepository.getCelestialBody(id) } returns Result.success(
            expectedBody
        )

        // When
        val result = getCelestialBodyUseCase(id)

        // Then
        coVerify(exactly = 1) { celestialBodiesRepository.getCelestialBody(id) }
        assertTrue(result.isSuccess)
        assertEquals(expectedBody, result.getOrNull())
    }

    @Test
    fun `cuando el cuerpo celeste no existe, devuelve null`() = runBlocking {
        // Given
        val id = "pluto"
        coEvery { celestialBodiesRepository.getCelestialBody(id) } returns Result.success(null)

        // When
        val result = getCelestialBodyUseCase(id)

        // Then
        coVerify(exactly = 1) { celestialBodiesRepository.getCelestialBody(id) }
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `cuando ocurre un error en el repository, devuelve un failure`() = runBlocking {
        // Given
        val id = "venus"
        val exception = RuntimeException("Error al obtener el cuerpo celeste")
        coEvery { celestialBodiesRepository.getCelestialBody(id) } returns Result.failure(exception)

        // When
        val result = getCelestialBodyUseCase(id)

        // Then
        coVerify(exactly = 1) { celestialBodiesRepository.getCelestialBody(id) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}