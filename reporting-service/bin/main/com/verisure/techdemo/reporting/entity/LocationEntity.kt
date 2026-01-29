package com.verisure.techdemo.reporting.entity

import jakarta.persistence.*
import java.time.Instant

/**
 * JPA Entity for Location Event Persistence.
 *
 * Represents a single location event stored in PostgreSQL.
 * This is the persistence model, separate from the domain [LocationEvent] model.
 *
 * ## Table Schema
 * ```sql
 * CREATE TABLE location_events (
 *     id          BIGSERIAL PRIMARY KEY,
 *     event_id    VARCHAR(100) NOT NULL,
 *     user_id     VARCHAR(100) NOT NULL,
 *     latitude    DOUBLE PRECISION NOT NULL,
 *     longitude   DOUBLE PRECISION NOT NULL,
 *     timestamp   TIMESTAMP WITH TIME ZONE NOT NULL,
 *     received_at TIMESTAMP WITH TIME ZONE NOT NULL
 * );
 * ```
 *
 * ## Indexes
 * Two indexes are defined for query optimization:
 * - `idx_user_id`: Optimizes user-specific queries
 * - `idx_timestamp`: Optimizes time-range queries and sorting
 *
 * ## Performance Notes
 * - Hibernate auto-generates the schema in dev (ddl-auto=update)
 * - For production, use Flyway/Liquibase migrations
 * - Consider partitioning by timestamp for large datasets
 *
 * @property id Auto-generated primary key
 * @property eventId Unique event identifier from the producer
 * @property userId User who generated this location
 * @property latitude GPS latitude coordinate
 * @property longitude GPS longitude coordinate
 * @property timestamp When the location was captured on device
 * @property receivedAt When the server received the event
 *
 * @see com.verisure.techdemo.models.LocationEvent Domain model
 * @see LocationRepository JPA repository for this entity
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Entity
@Table(
    name = "location_events",
    indexes = [
        Index(name = "idx_user_id", columnList = "userId"),
        Index(name = "idx_timestamp", columnList = "timestamp")
    ]
)
data class LocationEntity(
    /** Auto-generated database primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    /** Unique event identifier from the API Gateway. */
    @Column(nullable = false, length = 100)
    val eventId: String,
    
    /** User identifier who generated this location event. */
    @Column(nullable = false, length = 100)
    val userId: String,
    
    /** GPS latitude coordinate (-90 to 90). */
    @Column(nullable = false)
    val latitude: Double,
    
    /** GPS longitude coordinate (-180 to 180). */
    @Column(nullable = false)
    val longitude: Double,
    
    /** Device timestamp when location was captured. */
    @Column(nullable = false)
    val timestamp: Instant,
    
    /** Server timestamp when event was received. */
    @Column(nullable = false)
    val receivedAt: Instant = Instant.now()
)
