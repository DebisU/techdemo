package com.verisure.techdemo.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Data Transfer Object for Incoming Location Requests.
 *
 * Represents the HTTP request body for location submissions.
 * Includes built-in validation via Kotlin's `init` block.
 *
 * ## JSON Example
 * ```json
 * {
 *   "userId": "user-12345",
 *   "latitude": 40.4168,
 *   "longitude": -3.7038,
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 * ```
 *
 * ## Validation Rules
 * - `userId`: Must not be blank
 * - `latitude`: Must be in range [-90, 90]
 * - `longitude`: Must be in range [-180, 180]
 * - `timestamp`: Optional, defaults to current time
 *
 * ## Usage
 * ```kotlin
 * @PostMapping
 * fun submitLocation(@RequestBody request: LocationRequest): ResponseEntity<...> {
 *     // Validation happens automatically on construction
 *     val event = LocationEvent.from(request)
 *     // ...
 * }
 * ```
 *
 * @property userId Unique identifier for the user
 * @property latitude GPS latitude coordinate (-90 to 90 degrees)
 * @property longitude GPS longitude coordinate (-180 to 180 degrees)
 * @property timestamp When the location was captured (defaults to now)
 *
 * @throws IllegalArgumentException If validation constraints are violated
 *
 * @see LocationEvent Domain event created from this request
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
data class LocationRequest(
    /** Unique identifier for the user submitting the location. */
    @JsonProperty("userId")
    val userId: String,
    
    /** GPS latitude coordinate in degrees (-90 to 90). */
    @JsonProperty("latitude")
    val latitude: Double,
    
    /** GPS longitude coordinate in degrees (-180 to 180). */
    @JsonProperty("longitude")
    val longitude: Double,
    
    /** Timestamp when the location was captured on the device. */
    @JsonProperty("timestamp")
    val timestamp: Instant = Instant.now()
) {
    /**
     * Validates the request data on construction.
     * 
     * Throws [IllegalArgumentException] if any constraint is violated.
     * This provides fail-fast validation before any processing occurs.
     */
    init {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(latitude in -90.0..90.0) { "latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "longitude must be between -180 and 180" }
    }
}
