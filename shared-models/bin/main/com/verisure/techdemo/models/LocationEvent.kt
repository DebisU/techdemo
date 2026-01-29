package com.verisure.techdemo.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

/**
 * Domain Event for Location Tracking.
 *
 * Represents an immutable location event that flows through the system.
 * This is the core event published to RabbitMQ and consumed by all downstream services.
 *
 * ## Event Lifecycle
 * ```
 * [API Gateway]                    [RabbitMQ]                    [Consumers]
 *      │                               │                              │
 *      │ LocationRequest.toEvent()     │                              │
 *      ├──────────────────────────────►│ LocationEvent (JSON)         │
 *      │                               ├─────────────────────────────►│
 *      │                               │                              │
 * ```
 *
 * ## Serialization
 * Uses Jackson annotations for JSON serialization over RabbitMQ.
 * The `@JsonProperty` annotations ensure consistent field naming in JSON.
 *
 * ## Traceability
 * Each event has a unique [eventId] (UUID) generated at creation time.
 * This enables:
 * - End-to-end request tracing
 * - Duplicate detection
 * - Correlation in logs and metrics
 *
 * @property eventId Unique identifier for tracing (auto-generated UUID)
 * @property userId User who generated this location
 * @property latitude GPS latitude coordinate (-90 to 90)
 * @property longitude GPS longitude coordinate (-180 to 180)
 * @property timestamp Device timestamp when location was captured
 * @property receivedAt Server timestamp when event was created
 *
 * @see LocationRequest Incoming HTTP request DTO
 * @see com.verisure.techdemo.reporting.entity.LocationEntity Persistence model
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
data class LocationEvent(
    /** Unique event identifier for distributed tracing. */
    @JsonProperty("eventId")
    val eventId: String = UUID.randomUUID().toString(),
    
    /** User identifier who generated this location event. */
    @JsonProperty("userId")
    val userId: String,
    
    /** GPS latitude coordinate (-90 to 90). */
    @JsonProperty("latitude")
    val latitude: Double,
    
    /** GPS longitude coordinate (-180 to 180). */
    @JsonProperty("longitude")
    val longitude: Double,
    
    /** Device timestamp when the location was captured. */
    @JsonProperty("timestamp")
    val timestamp: Instant,
    
    /** Server timestamp when the event was received and created. */
    @JsonProperty("receivedAt")
    val receivedAt: Instant = Instant.now()
) {
    companion object {
        /**
         * Factory method to create a LocationEvent from an HTTP request.
         *
         * Transforms the incoming [LocationRequest] into a domain event
         * with auto-generated eventId and receivedAt timestamp.
         *
         * @param request The validated HTTP request DTO
         * @return A new LocationEvent ready for publishing to RabbitMQ
         */
        fun from(request: LocationRequest): LocationEvent {
            return LocationEvent(
                userId = request.userId,
                latitude = request.latitude,
                longitude = request.longitude,
                timestamp = request.timestamp
            )
        }
    }
}
