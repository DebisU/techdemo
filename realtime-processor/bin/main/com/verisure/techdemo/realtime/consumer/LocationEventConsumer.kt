package com.verisure.techdemo.realtime.consumer

import com.verisure.techdemo.models.LocationEvent
import com.verisure.techdemo.realtime.config.RabbitMQConfig
import com.verisure.techdemo.realtime.service.RealtimeAnalyticsService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * RabbitMQ Consumer for Real-time Location Processing.
 *
 * Listens to the realtime queue and delegates event processing to [RealtimeAnalyticsService].
 * This class is intentionally simple (thin controller pattern) - all business logic
 * resides in the service layer.
 *
 * ## Consumer Configuration
 * The consumer behavior is configured in [RabbitMQConfig]:
 * - Concurrent consumers: 3-10 (auto-scaling)
 * - Prefetch count: 50 messages (batching for throughput)
 * - Auto-ack: Enabled (trade-off for performance)
 *
 * ## Message Flow
 * ```
 * [RabbitMQ] → LocationEventConsumer → RealtimeAnalyticsService → [Redis]
 * ```
 *
 * @property analyticsService Service for real-time event analytics
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Component
class LocationEventConsumer(
    private val analyticsService: RealtimeAnalyticsService
) {
    private val logger = LoggerFactory.getLogger(LocationEventConsumer::class.java)

    /**
     * Handles incoming location events from RabbitMQ.
     *
     * The [RabbitListener] annotation configures this method as a message handler.
     * Spring AMQP automatically deserializes JSON messages into [LocationEvent] objects.
     *
     * @param event The deserialized location event from the queue
     */
    @RabbitListener(queues = [RabbitMQConfig.REALTIME_QUEUE])
    fun handleLocationEvent(event: LocationEvent) {
        logger.debug("Received event: {} from user: {}", event.eventId, event.userId)
        analyticsService.processEvent(event)
    }
}
