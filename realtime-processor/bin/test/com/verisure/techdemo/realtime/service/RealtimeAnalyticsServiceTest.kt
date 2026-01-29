package com.verisure.techdemo.realtime.service

import com.verisure.techdemo.models.LocationEvent
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ListOperations
import java.time.Instant

class RealtimeAnalyticsServiceTest {

    @Test
    fun `should detect anomaly for large distance jump`() {
        val redisTemplate = mock<RedisTemplate<String, Any>>()
        val valueOps = mock<ValueOperations<String, Any>>()
        val listOps = mock<ListOperations<String, Any>>()
        val meterRegistry = SimpleMeterRegistry()
        
        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
        whenever(redisTemplate.opsForList()).thenReturn(listOps)
        
        // Previous location in New York
        val previousLocation = mapOf(
            "latitude" to 40.7128,
            "longitude" to -74.0060,
            "timestamp" to Instant.now().toString()
        )
        whenever(valueOps.get(any())).thenReturn(previousLocation)
        
        val service = RealtimeAnalyticsService(redisTemplate, meterRegistry)
        
        // Current location in London (5500+ km away)
        val event = LocationEvent(
            userId = "1",
            latitude = 51.5074,
            longitude = -0.1278,
            timestamp = Instant.now()
        )
        
        val anomalyCountBefore = meterRegistry.counter("location.realtime.anomalies").count()
        service.processEvent(event)
        val anomalyCountAfter = meterRegistry.counter("location.realtime.anomalies").count()
        
        // Should detect anomaly
        assertEquals(anomalyCountBefore + 1, anomalyCountAfter)
        verify(listOps).rightPush(eq("anomalies:1"), eq(event.eventId))
    }

    @Test
    fun `should not detect anomaly for short distance`() {
        val redisTemplate = mock<RedisTemplate<String, Any>>()
        val valueOps = mock<ValueOperations<String, Any>>()
        val listOps = mock<ListOperations<String, Any>>()
        val meterRegistry = SimpleMeterRegistry()
        
        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
        whenever(redisTemplate.opsForList()).thenReturn(listOps)
        
        // Previous location
        val previousLocation = mapOf(
            "latitude" to 40.7128,
            "longitude" to -74.0060,
            "timestamp" to Instant.now().toString()
        )
        whenever(valueOps.get(any())).thenReturn(previousLocation)
        
        val service = RealtimeAnalyticsService(redisTemplate, meterRegistry)
        
        // Current location nearby (1 km)
        val event = LocationEvent(
            userId = "1",
            latitude = 40.7228,
            longitude = -74.0160,
            timestamp = Instant.now()
        )
        
        val anomalyCountBefore = meterRegistry.counter("location.realtime.anomalies").count()
        service.processEvent(event)
        val anomalyCountAfter = meterRegistry.counter("location.realtime.anomalies").count()
        
        // Should NOT detect anomaly
        assertEquals(anomalyCountBefore, anomalyCountAfter)
        verify(listOps, never()).rightPush(any(), any())
    }
}
