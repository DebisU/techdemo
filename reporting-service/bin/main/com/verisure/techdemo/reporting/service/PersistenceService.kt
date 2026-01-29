package com.verisure.techdemo.reporting.service

import com.verisure.techdemo.models.LocationEvent
import com.verisure.techdemo.reporting.entity.LocationEntity
import com.verisure.techdemo.reporting.repository.LocationRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Persistence Service for Location Events.
 *
 * Responsible for storing location events in PostgreSQL with:
 * - Transaction management for data integrity
 * - Micrometer metrics for observability
 * - Entity mapping from domain to persistence model
 *
 * ## Performance Considerations
 * While individual saves work well at our target throughput (1000 req/s),
 * consider batch inserts for higher loads:
 * ```kotlin
 * @Transactional
 * fun saveBatch(events: List<LocationEvent>) {
 *     repository.saveAll(events.map { it.toEntity() })
 * }
 * ```
 *
 * ## Metrics
 * - `location.reporting.saved`: Counter of successfully saved events
 *
 * ## Transaction Behavior
 * Each [saveLocation] call runs in its own transaction. On exception:
 * - Transaction is rolled back
 * - Exception is re-thrown for upstream handling
 * - Event remains in RabbitMQ queue (if using manual ack)
 *
 * @property repository JPA repository for database operations
 * @property meterRegistry Micrometer registry for metrics
 *
 * @see LocationEntity JPA entity for persistence
 * @see LocationRepository Spring Data JPA repository
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Service
class PersistenceService(
    private val repository: LocationRepository,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(PersistenceService::class.java)
    
    /**
     * Counter for successfully saved events.
     * 
     * Exposed via Prometheus endpoint at `/actuator/prometheus` with metric name
     * `location_reporting_saved_total`.
     */
    private val savedCounter: Counter = Counter.builder("location.reporting.saved")
        .description("Total events saved to database")
        .register(meterRegistry)

    /**
     * Persists a location event to PostgreSQL.
     *
     * Maps the domain [LocationEvent] to a JPA [LocationEntity] and saves it.
     * Runs within a transaction that will roll back on any exception.
     *
     * @param event The location event to persist
     * @throws Exception Re-throws any persistence exception after logging
     */
    @Transactional
    fun saveLocation(event: LocationEvent) {
        try {
            val entity = LocationEntity(
                eventId = event.eventId,
                userId = event.userId,
                latitude = event.latitude,
                longitude = event.longitude,
                timestamp = event.timestamp,
                receivedAt = event.receivedAt
            )
            
            repository.save(entity)
            savedCounter.increment()
            logger.debug("Saved event {} to database", event.eventId)
            
        } catch (e: Exception) {
            logger.error("Failed to save event: {}", event.eventId, e)
            throw e
        }
    }
}
