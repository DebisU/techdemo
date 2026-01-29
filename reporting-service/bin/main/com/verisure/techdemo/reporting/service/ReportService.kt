package com.verisure.techdemo.reporting.service

import com.verisure.techdemo.models.LocationReport
import com.verisure.techdemo.models.ReportResponse
import com.verisure.techdemo.reporting.repository.LocationRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Report Generation Service.
 *
 * Generates aggregated analytics reports from the location data stored in PostgreSQL.
 * Uses Redis caching to improve response times for repeated requests.
 *
 * ## Caching Strategy
 * - **Cache Name**: `locationReports`
 * - **TTL**: 60 seconds (configured in application.properties)
 * - **Invalidation**: Every 100 events (see [LocationEventConsumer])
 *
 * ## Report Types
 * 1. **Global Report**: Aggregated stats for all users
 * 2. **User Report**: Detailed location history for a specific user
 *
 * ## Query Performance
 * The [generateReport] method uses a native query with aggregations:
 * ```sql
 * SELECT user_id, COUNT(*), MAX(timestamp), last_lat, last_lon
 * FROM locations
 * GROUP BY user_id
 * ```
 * Consider adding indexes on `user_id` and `timestamp` for large datasets.
 *
 * @property repository JPA repository for database queries
 *
 * @see LocationReport Individual user report data
 * @see ReportResponse Aggregated response with all reports
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Service
class ReportService(
    private val repository: LocationRepository
) {
    private val logger = LoggerFactory.getLogger(ReportService::class.java)

    /**
     * Generates an aggregated report for all users.
     *
     * This is the main reporting endpoint, cached for 60 seconds.
     * Returns total counts and per-user location summaries.
     *
     * ## Cache Behavior
     * - Results are cached in Redis under key `locationReports::SimpleKey[]`
     * - Cache is skipped if result is null (defensive)
     * - Manually invalidated by [LocationEventConsumer] every 100 events
     *
     * @return Complete report with user summaries and global statistics
     */
    @Cacheable("locationReports", unless = "#result == null")
    fun generateReport(): ReportResponse {
        logger.info("Generating location report...")
        
        val totalUsers = repository.countDistinctUsers()
        val totalEvents = repository.count()
        
        val stats = repository.getAggregatedStats()
        
        val reports = stats.map { row ->
            // Note: Array indices correspond to the native query SELECT clause
            // [0]=user_id, [1]=count, [2]=max_timestamp, [3]=last_lat, [4]=last_lon
            LocationReport(
                userId = row[0] as String,
                totalLocations = (row[1] as Long),
                lastLatitude = (row[3] as Number).toDouble(),
                lastLongitude = (row[4] as Number).toDouble(),
                lastUpdated = row[2] as Instant
            )
        }
        
        return ReportResponse(
            totalUsers = totalUsers,
            totalEvents = totalEvents,
            reports = reports
        )
    }
    
    /**
     * Generates a report for a specific user.
     *
     * Unlike the global report, this is not cached because:
     * - Each userId creates a unique cache key
     * - User-specific data changes frequently
     * - Memory overhead for many users would be excessive
     *
     * @param userId The user identifier to generate report for
     * @return List containing the user's location summary, or empty if not found
     */
    fun getUserReport(userId: String): List<LocationReport> {
        val locations = repository.findByUserId(userId)
        
        if (locations.isEmpty()) {
            return emptyList()
        }
        
        // Find the most recent location for the user
        val lastLocation = locations.maxByOrNull { it.timestamp }
            ?: return emptyList()
        
        return listOf(
            LocationReport(
                userId = userId,
                totalLocations = locations.size.toLong(),
                lastLatitude = lastLocation.latitude,
                lastLongitude = lastLocation.longitude,
                lastUpdated = lastLocation.timestamp
            )
        )
    }
}
