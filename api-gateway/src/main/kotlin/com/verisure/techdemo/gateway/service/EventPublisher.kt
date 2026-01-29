package com.verisure.techdemo.gateway.service

import com.verisure.techdemo.gateway.config.RabbitMQConfig
import com.verisure.techdemo.models.LocationEvent
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

/**
 * Event Publisher Service for RabbitMQ.
 *
 * Responsible for publishing [LocationEvent] instances to the RabbitMQ exchange.
 * The exchange uses a fanout topology, meaning each event is delivered to ALL
 * bound queues (realtime-processor and reporting-service).
 *
 * ## Message Flow
 * ```
 * EventPublisher → [location.exchange] → [location.realtime.queue]
 *                                      → [location.reporting.queue]
 * ```
 *
 * ## Reliability Considerations
 * - Messages are serialized to JSON via Jackson
 * - Publisher confirms can be enabled for guaranteed delivery
 * - Failed publishes throw [PublishException] for proper error handling
 *
 * ## Metrics
 * - `location.events.published` - Counter of successfully published events
 *
 * @property rabbitTemplate Spring AMQP template for message publishing
 * @property meterRegistry Micrometer registry for metrics
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Service
class EventPublisher(
    private val rabbitTemplate: RabbitTemplate,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)
    
    /** Counter for tracking successfully published events */
    private val publishedCounter: Counter = Counter.builder("location.events.published")
        .description("Total location events published to RabbitMQ")
        .register(meterRegistry)

    /**
     * Publishes a location event to the RabbitMQ fanout exchange.
     *
     * The event is serialized to JSON and sent to the location exchange.
     * Due to fanout topology, all consumers (realtime and reporting) receive the event.
     *
     * @param event The location event to publish
     * @throws PublishException if the publish operation fails
     *
     * @sample
     * ```kotlin
     * val event = LocationEvent.from(request)
     * eventPublisher.publish(event)
     * ```
     */
    fun publish(event: LocationEvent) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.LOCATION_EXCHANGE, "", event)
            publishedCounter.increment()
            logger.debug("Published event: {} for user: {}", event.eventId, event.userId)
        } catch (e: Exception) {
            logger.error("Failed to publish event: {}", event.eventId, e)
            throw PublishException("Failed to publish location event", e)
        }
    }
}

/**
 * Exception thrown when event publishing to RabbitMQ fails.
 *
 * This exception wraps the underlying cause (connection error, serialization error, etc.)
 * and provides a clear indication that the message queue operation failed.
 *
 * @property message Human-readable error description
 * @property cause The underlying exception that caused the failure
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
class PublishException(message: String, cause: Throwable) : RuntimeException(message, cause)
