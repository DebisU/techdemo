package com.verisure.techdemo.gateway.service

import com.verisure.techdemo.gateway.config.RabbitMQConfig
import com.verisure.techdemo.models.LocationEvent
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.Instant

@SpringBootTest
class EventPublisherTest {

    @MockBean
    private lateinit var rabbitTemplate: RabbitTemplate

    @Test
    fun `should publish event to correct exchange`() {
        val meterRegistry = SimpleMeterRegistry()
        val publisher = EventPublisher(rabbitTemplate, meterRegistry)
        
        val event = LocationEvent(
            userId = "1",
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = Instant.now()
        )
        
        publisher.publish(event)
        
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.LOCATION_EXCHANGE),
            eq(""),
            eq(event)
        )
    }

    @Test
    fun `should increment metrics on publish`() {
        val meterRegistry = SimpleMeterRegistry()
        val publisher = EventPublisher(rabbitTemplate, meterRegistry)
        
        val event = LocationEvent(
            userId = "1",
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = Instant.now()
        )
        
        val counterBefore = meterRegistry.counter("location.events.published").count()
        publisher.publish(event)
        val counterAfter = meterRegistry.counter("location.events.published").count()
        
        assertEquals(counterBefore + 1, counterAfter)
    }
}
