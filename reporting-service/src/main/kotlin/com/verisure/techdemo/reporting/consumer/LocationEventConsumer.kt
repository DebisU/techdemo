package com.verisure.techdemo.reporting.consumer

import com.verisure.techdemo.models.LocationEvent
import com.verisure.techdemo.reporting.config.RabbitMQConfig
import com.verisure.techdemo.reporting.service.PersistenceService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

/**
 * RabbitMQ Consumer for Reporting Service.
 *
 * Consumes location events from the reporting queue and orchestrates:
 * - Event persistence to PostgreSQL via [PersistenceService]
 * - Cache invalidation to keep reports fresh
 *
 * ## Thread Safety
 * This consumer runs with multiple concurrent consumers (2-5) as configured
 * in [RabbitMQConfig]. The [eventCounter] uses [AtomicInteger] to ensure
 * thread-safe counting across concurrent message handlers.
 *
 * ## Cache Strategy
 * Invalidates the `locationReports` cache every [CACHE_INVALIDATION_THRESHOLD]
 * events to balance:
 * - **Freshness**: Reports reflect recent data
 * - **Performance**: Avoids invalidating on every single event
 *
 * ## Message Flow
 * ```
 * [RabbitMQ] → LocationEventConsumer → PersistenceService → [PostgreSQL]
 *                     │
 *                     └── Cache invalidation → [Redis]
 * ```
 *
 * @property persistenceService Service for database operations
 * @property cacheManager Spring cache manager for Redis cache access
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Component
class LocationEventConsumer(
    private val persistenceService: PersistenceService,
    private val cacheManager: CacheManager
) {
    private val logger = LoggerFactory.getLogger(LocationEventConsumer::class.java)
    
    /**
     * Thread-safe event counter for cache invalidation decisions.
     * 
     * Uses [AtomicInteger] because this consumer runs with multiple
     * concurrent threads processing messages in parallel.
     */
    private val eventCounter = AtomicInteger(0)

    companion object {
        /**
         * Number of events between cache invalidations.
         * 
         * Trade-off between report freshness and cache hit rate.
         * Lower values = fresher data but more cache misses.
         */
        private const val CACHE_INVALIDATION_THRESHOLD = 100
        
        /** Name of the cache storing report results. */
        private const val REPORTS_CACHE_NAME = "locationReports"
    }

    /**
     * Handles incoming location events from RabbitMQ.
     *
     * Processing steps:
     * 1. Persists the event to PostgreSQL
     * 2. Increments the event counter atomically
     * 3. Invalidates cache if threshold reached
     *
     * @param event The deserialized location event from the queue
     */
    @RabbitListener(queues = [RabbitMQConfig.REPORTING_QUEUE])
    fun handleLocationEvent(event: LocationEvent) {
        logger.debug("Received event: {} from user: {}", event.eventId, event.userId)
        persistenceService.saveLocation(event)
        
        // Thread-safe increment and check for cache invalidation
        val currentCount = eventCounter.incrementAndGet()
        if (currentCount % CACHE_INVALIDATION_THRESHOLD == 0) {
            cacheManager.getCache(REPORTS_CACHE_NAME)?.clear()
            logger.debug("Cache invalidated after {} events", currentCount)
        }
    }
}
