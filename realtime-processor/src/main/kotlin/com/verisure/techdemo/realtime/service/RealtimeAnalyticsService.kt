package com.verisure.techdemo.realtime.service

import com.verisure.techdemo.models.LocationEvent
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time Analytics Service for Location Events.
 *
 * Performs streaming analytics on incoming location events:
 * - Caches recent locations in Redis for fast retrieval
 * - Detects movement anomalies using the Haversine formula
 * - Tracks user activity statistics
 * - Calculates movement patterns and velocity
 *
 * ## Anomaly Detection
 * An anomaly is detected when a user's location jumps more than [ANOMALY_DISTANCE_THRESHOLD_KM]
 * kilometers between consecutive updates. This could indicate:
 * - GPS spoofing
 * - Device handoff
 * - Data quality issues
 *
 * ## Redis Data Model
 * ```
 * location:{userId}:last     → Last known location (HashMap)
 * anomalies:{userId}         → List of anomalous event IDs
 * stats:user:{userId}:count  → User event counter
 * stats:global:events        → Global event counter
 * ```
 *
 * ## Metrics Exposed
 * - `location.realtime.processed` - Total events processed
 * - `location.realtime.anomalies` - Anomalies detected
 *
 * @property redisTemplate Redis template for cache operations
 * @property meterRegistry Micrometer registry for metrics
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Service
class RealtimeAnalyticsService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(RealtimeAnalyticsService::class.java)
    
    /** Counter for total events processed in real-time */
    private val processedCounter: Counter = Counter.builder("location.realtime.processed")
        .description("Total events processed in realtime")
        .register(meterRegistry)
    
    /** Counter for detected anomalies */
    private val anomalyCounter: Counter = Counter.builder("location.realtime.anomalies")
        .description("Anomalies detected (large distance jumps)")
        .register(meterRegistry)

    companion object {
        /** Distance threshold in kilometers for anomaly detection */
        private const val ANOMALY_DISTANCE_THRESHOLD_KM = 100.0
        
        /** Time-to-live for cached location data */
        private val LOCATION_CACHE_TTL: Duration = Duration.ofHours(1)
        
        /** Earth's radius in kilometers (for Haversine calculation) */
        private const val EARTH_RADIUS_KM = 6371.0
    }

    /**
     * Processes a location event in real-time.
     *
     * Processing steps:
     * 1. Increment metrics counter
     * 2. Retrieve previous location from Redis (if exists)
     * 3. Store current location in Redis with TTL
     * 4. Check for anomalies (distance jump detection)
     * 5. Update activity statistics
     *
     * @param event The location event to process
     */
    fun processEvent(event: LocationEvent) {
        try {
            processedCounter.increment()
            
            val key = "location:${event.userId}:last"
            val previousLocation = redisTemplate.opsForValue().get(key) as? Map<*, *>
            
            // Store current location with TTL
            redisTemplate.opsForValue().set(
                key,
                mapOf(
                    "latitude" to event.latitude,
                    "longitude" to event.longitude,
                    "timestamp" to event.timestamp.toString()
                ),
                LOCATION_CACHE_TTL
            )
            
            // Anomaly detection: check for large distance jumps
            if (previousLocation != null) {
                detectAnomaly(event, previousLocation)
            }
            
            // Update activity counters
            redisTemplate.opsForValue().increment("stats:user:${event.userId}:count")
            redisTemplate.opsForValue().increment("stats:global:events")
            
            logger.debug("Processed event {} for user {}", event.eventId, event.userId)
            
        } catch (e: Exception) {
            logger.error("Error processing event: {}", event.eventId, e)
            // Note: In production, consider retry mechanism or dead-letter queue
        }
    }
    
    /**
     * Detects anomalous movement patterns.
     *
     * Compares current location with previous location and flags events
     * where the distance exceeds the threshold.
     *
     * @param event Current location event
     * @param previousLocation Map containing previous lat/lon
     */
    private fun detectAnomaly(event: LocationEvent, previousLocation: Map<*, *>) {
        val prevLat = (previousLocation["latitude"] as Number).toDouble()
        val prevLon = (previousLocation["longitude"] as Number).toDouble()
        val distance = calculateHaversineDistance(prevLat, prevLon, event.latitude, event.longitude)
        
        if (distance > ANOMALY_DISTANCE_THRESHOLD_KM) {
            logger.warn("ANOMALY: User {} moved {:.2f} km", event.userId, distance)
            anomalyCounter.increment()
            
            // Store anomaly for later analysis
            redisTemplate.opsForList().rightPush("anomalies:${event.userId}", event.eventId)
        }
    }
    
    /**
     * Calculates the great-circle distance between two geographic points.
     *
     * Uses the Haversine formula, which accounts for the Earth's spherical shape.
     * This provides accurate results for any two points on Earth.
     *
     * ## Formula
     * ```
     * a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlon/2)
     * c = 2 × atan2(√a, √(1−a))
     * d = R × c
     * ```
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance between the points in kilometers
     *
     * @see <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine formula</a>
     */
    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_KM * c
    }
}
