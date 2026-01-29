package com.verisure.techdemo.reporting.service

import com.verisure.techdemo.models.LocationEvent
import com.verisure.techdemo.reporting.entity.LocationEntity
import com.verisure.techdemo.reporting.repository.LocationRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class PersistenceServiceTest {

    @Test
    fun `should save location event to database`() {
        val repository = mock<LocationRepository>()
        val meterRegistry = SimpleMeterRegistry()
        val service = PersistenceService(repository, meterRegistry)
        
        val event = LocationEvent(
            eventId = "event123",
            userId = "1",
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = Instant.now()
        )
        
        service.saveLocation(event)
        
        verify(repository).save(argThat { entity: LocationEntity ->
            entity.eventId == event.eventId &&
            entity.userId == event.userId &&
            entity.latitude == event.latitude &&
            entity.longitude == event.longitude
        })
    }

    @Test
    fun `should increment metrics on save`() {
        val repository = mock<LocationRepository>()
        val meterRegistry = SimpleMeterRegistry()
        val service = PersistenceService(repository, meterRegistry)
        
        val event = LocationEvent(
            userId = "1",
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = Instant.now()
        )
        
        val counterBefore = meterRegistry.counter("location.reporting.saved").count()
        service.saveLocation(event)
        val counterAfter = meterRegistry.counter("location.reporting.saved").count()
        
        assertEquals(counterBefore + 1, counterAfter)
    }
}
