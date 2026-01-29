package com.verisure.techdemo.reporting.repository

import com.verisure.techdemo.reporting.entity.LocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA Repository for Location Events.
 *
 * Provides database access methods for [LocationEntity] with custom
 * JPQL queries for aggregated reporting.
 *
 * ## Inherited Methods
 * From [JpaRepository]:
 * - `save(entity)`: Persist a location event
 * - `count()`: Total number of events
 * - `findAll()`: All events (use with caution)
 * - `deleteAll()`: Clear all events
 *
 * ## Query Performance
 * The [getAggregatedStats] query performs grouping across all records.
 * For large datasets (>1M rows), consider:
 * - Materialized views
 * - Pre-computed aggregates
 * - Time-bounded queries
 *
 * @see LocationEntity JPA entity for persistence
 * @see com.verisure.techdemo.reporting.service.ReportService Consumer of this repository
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Repository
interface LocationRepository : JpaRepository<LocationEntity, Long> {
    
    /**
     * Finds all location events for a specific user.
     *
     * Uses the `idx_user_id` index for optimized lookups.
     *
     * @param userId The user identifier to query
     * @return List of all location events for the user, empty if none found
     */
    fun findByUserId(userId: String): List<LocationEntity>
    
    /**
     * Retrieves aggregated statistics grouped by user.
     *
     * Returns an array for each user with:
     * - `[0]` userId (String)
     * - `[1]` count of locations (Long)
     * - `[2]` most recent timestamp (Instant)
     * - `[3]` last latitude (Double) - Note: uses MAX which may not be accurate
     * - `[4]` last longitude (Double) - Note: uses MAX which may not be accurate
     *
     * ## Known Limitation
     * Using MAX(latitude/longitude) doesn't guarantee the coordinates
     * from the most recent event. For production, consider a window
     * function or subquery to get actual last coordinates.
     *
     * @return List of Object arrays, one per user
     */
    @Query("""
        SELECT l.userId, COUNT(l) as count, MAX(l.timestamp) as lastUpdate,
               MAX(l.latitude) as lastLat, MAX(l.longitude) as lastLon
        FROM LocationEntity l
        GROUP BY l.userId
    """)
    fun getAggregatedStats(): List<Array<Any>>
    
    /**
     * Counts the number of distinct users with location data.
     *
     * @return Number of unique users in the database
     */
    @Query("SELECT COUNT(DISTINCT l.userId) FROM LocationEntity l")
    fun countDistinctUsers(): Long
}
