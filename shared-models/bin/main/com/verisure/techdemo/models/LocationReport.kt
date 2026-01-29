package com.verisure.techdemo.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Report DTO for Individual User Location Summary.
 *
 * Contains aggregated location statistics for a single user.
 * Used in both global reports and user-specific queries.
 *
 * ## JSON Example
 * ```json
 * {
 *   "userId": "user-12345",
 *   "totalLocations": 150,
 *   "lastLatitude": 40.4168,
 *   "lastLongitude": -3.7038,
 *   "lastUpdated": "2024-01-15T10:30:00Z"
 * }
 * ```
 *
 * @property userId Unique identifier of the user
 * @property totalLocations Total number of location events recorded for this user
 * @property lastLatitude Latitude of the most recent location
 * @property lastLongitude Longitude of the most recent location
 * @property lastUpdated Timestamp of the most recent location event
 *
 * @see ReportResponse Aggregated response containing multiple reports
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
data class LocationReport(
    /** User identifier. */
    @JsonProperty("userId")
    val userId: String,
    
    /** Total count of location events for this user. */
    @JsonProperty("totalLocations")
    val totalLocations: Long,
    
    /** Latitude of the most recent location. */
    @JsonProperty("lastLatitude")
    val lastLatitude: Double,
    
    /** Longitude of the most recent location. */
    @JsonProperty("lastLongitude")
    val lastLongitude: Double,
    
    /** Timestamp of the most recent location event. */
    @JsonProperty("lastUpdated")
    val lastUpdated: Instant
)

/**
 * Aggregated Report Response DTO.
 *
 * Top-level response for the global report endpoint containing:
 * - System-wide statistics (total users, total events)
 * - Per-user location summaries
 * - Generation timestamp
 *
 * ## JSON Example
 * ```json
 * {
 *   "totalUsers": 25,
 *   "totalEvents": 5000,
 *   "reports": [...],
 *   "generatedAt": "2024-01-15T10:30:00Z"
 * }
 * ```
 *
 * ## Caching
 * This response is cached in Redis with a 60-second TTL.
 * The [generatedAt] field helps clients understand data freshness.
 *
 * @property totalUsers Number of unique users with location data
 * @property totalEvents Total number of location events in the system
 * @property reports List of per-user location summaries
 * @property generatedAt When this report was generated (useful for caching awareness)
 *
 * @see LocationReport Individual user report data
 * @see com.verisure.techdemo.reporting.service.ReportService Report generation
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
data class ReportResponse(
    /** Total number of unique users with location data. */
    @JsonProperty("totalUsers")
    val totalUsers: Long,
    
    /** Total number of location events in the system. */
    @JsonProperty("totalEvents")
    val totalEvents: Long,
    
    /** List of per-user location summaries. */
    @JsonProperty("reports")
    val reports: List<LocationReport>,
    
    /** Timestamp when this report was generated. */
    @JsonProperty("generatedAt")
    val generatedAt: Instant = Instant.now()
)
