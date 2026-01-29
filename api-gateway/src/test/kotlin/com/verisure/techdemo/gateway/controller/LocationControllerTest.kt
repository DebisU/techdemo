package com.verisure.techdemo.gateway.controller

import com.verisure.techdemo.gateway.service.EventPublisher
import com.verisure.techdemo.models.LocationRequest
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant

@WebMvcTest(LocationController::class)
class LocationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var eventPublisher: EventPublisher

    @MockBean
    private lateinit var meterRegistry: SimpleMeterRegistry

    @Test
    fun `should accept valid location request`() {
        val json = """
            {
                "userId": "1",
                "latitude": 40.7128,
                "longitude": -74.0060,
                "timestamp": "${Instant.now()}"
            }
        """.trimIndent()

        mockMvc.post("/api/locations") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isAccepted() }
            jsonPath("$.status") { value("accepted") }
            jsonPath("$.eventId") { exists() }
        }

        verify(eventPublisher).publish(any())
    }

    @Test
    fun `should reject invalid latitude`() {
        val json = """
            {
                "userId": "1",
                "latitude": 999.0,
                "longitude": -74.0060,
                "timestamp": "${Instant.now()}"
            }
        """.trimIndent()

        mockMvc.post("/api/locations") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { is5xxServerError() }
        }
    }

    @Test
    fun `should reject empty userId`() {
        val json = """
            {
                "userId": "",
                "latitude": 40.7128,
                "longitude": -74.0060,
                "timestamp": "${Instant.now()}"
            }
        """.trimIndent()

        mockMvc.post("/api/locations") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { is5xxServerError() }
        }
    }
}
