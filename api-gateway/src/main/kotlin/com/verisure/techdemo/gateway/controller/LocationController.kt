package com.verisure.techdemo.gateway.controller

import com.verisure.techdemo.gateway.service.EventPublisher
import com.verisure.techdemo.models.LocationEvent
import com.verisure.techdemo.models.LocationRequest
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for Location Event Ingestion.
 *
 * Handles incoming location updates from mobile devices and IoT sensors.
 * Implements the async processing pattern by immediately accepting requests
 * and delegating processing to downstream services via message queue.
 *
 * ## Endpoints
 * - `POST /api/locations` - Submit a new location update
 * - `GET /api/locations/health` - Service health check
 *
 * ## Metrics Exposed
 * - `location.requests.total` - Counter of total requests received
 * - `location.requests.duration` - Timer for request processing latency
 *
 * ## Example Request
 * ```json
 * POST /api/locations
 * {
 *   "userId": "user-123",
 *   "latitude": 40.4168,
 *   "longitude": -3.7038,
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 * ```
 *
 * @property eventPublisher Service for publishing events to RabbitMQ
 * @property meterRegistry Micrometer registry for metrics collection
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/locations")
class LocationController(
    private val eventPublisher: EventPublisher,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(LocationController::class.java)
    
    /** Counter for total location requests received */
    private val requestCounter: Counter = Counter.builder("location.requests.total")
        .description("Total location requests received")
        .register(meterRegistry)
    
    /** Timer for measuring request processing duration */
    private val requestTimer: Timer = Timer.builder("location.requests.duration")
        .description("Location request processing time")
        .register(meterRegistry)

    /**
     * Receives and processes a location update request.
     *
     * The method follows the async processing pattern:
     * 1. Validates the incoming request (via Bean Validation)
     * 2. Transforms the request into a domain event
     * 3. Publishes the event to RabbitMQ
     * 4. Returns 202 Accepted immediately (processing continues async)
     *
     * @param request The location data to process (validated automatically)
     * @return ResponseEntity with:
     *   - 202 Accepted: Event queued successfully, includes eventId for tracing
     *   - 400 Bad Request: Invalid input data (handled by Spring validation)
     *   - 500 Internal Server Error: Failed to publish to message queue
     *
     * @throws PublishException if RabbitMQ publish fails (caught and returned as 500)
     */
    @PostMapping
    fun receiveLocation(@Valid @RequestBody request: LocationRequest): ResponseEntity<Map<String, String>> {
        return requestTimer.recordCallable {
            try {
                requestCounter.increment()
                logger.info("Received location for user: {}", request.userId)
                
                val event = LocationEvent.from(request)
                eventPublisher.publish(event)
                
                ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    mapOf(
                        "status" to "accepted",
                        "eventId" to event.eventId,
                        "message" to "Location event queued for processing"
                    )
                )
            } catch (e: Exception) {
                logger.error("Error processing location request", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    mapOf(
                        "status" to "error",
                        "message" to "Failed to process location request"
                    )
                )
            }
        } ?: ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf("status" to "error", "message" to "Timer execution failed")
        )
    }

    /**
     * Health check endpoint for load balancers and orchestrators.
     *
     * @return 200 OK with status "UP" if service is healthy
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP"))
    }
}
