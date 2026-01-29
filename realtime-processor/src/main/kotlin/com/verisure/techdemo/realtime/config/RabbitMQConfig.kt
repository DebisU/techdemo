package com.verisure.techdemo.realtime.config

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * RabbitMQ Consumer Configuration for Real-time Processing.
 *
 * This configuration establishes the consumer side of the messaging infrastructure,
 * complementing the producer configuration in api-gateway.
 *
 * ## Queue Configuration
 * The realtime queue is declared as durable to survive broker restarts.
 * The binding to the fanout exchange is done in the api-gateway RabbitMQConfig.
 *
 * ## Consumer Tuning
 * Configured for high-throughput real-time processing:
 * - **Concurrent Consumers**: 3-10 (auto-scales based on load)
 * - **Prefetch Count**: 50 (batches messages for efficiency)
 * - **Auto-ack**: Enabled (trade-off between throughput and delivery guarantees)
 *
 * ## Performance Considerations
 * ```
 * Prefetch = 50: Each consumer fetches 50 messages at once
 * Max throughput = 10 consumers × 50 prefetch = 500 in-flight messages
 * ```
 *
 * For at-least-once delivery, consider setting:
 * - `acknowledgeMode = AcknowledgeMode.MANUAL`
 * - Lower prefetch count (10-20)
 *
 * @see com.verisure.techdemo.gateway.config.RabbitMQConfig Producer-side configuration
 * @see com.verisure.techdemo.realtime.consumer.LocationEventConsumer Message consumer
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Configuration
class RabbitMQConfig {

    companion object {
        /**
         * Queue name for real-time event processing.
         * 
         * This queue receives events via fanout binding and processes them
         * immediately for anomaly detection and caching.
         */
        const val REALTIME_QUEUE = "location.realtime.queue"
        
        /** Minimum number of concurrent consumers. */
        private const val MIN_CONCURRENT_CONSUMERS = 3
        
        /** Maximum number of concurrent consumers (auto-scales). */
        private const val MAX_CONCURRENT_CONSUMERS = 10
        
        /** Number of messages to prefetch per consumer. */
        private const val PREFETCH_COUNT = 50
    }

    /**
     * Declares the realtime queue with durability.
     *
     * Durable queues survive broker restarts, ensuring no message loss
     * during maintenance windows.
     *
     * @return A durable queue instance for real-time processing
     */
    @Bean
    fun realtimeQueue(): Queue {
        return Queue(REALTIME_QUEUE, true) // durable = true
    }

    /**
     * Configures JSON message converter for RabbitMQ.
     *
     * Uses Jackson for automatic serialization/deserialization of [LocationEvent]
     * objects to/from JSON format.
     *
     * @return Jackson-based message converter
     */
    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    /**
     * Configures the RabbitMQ listener container factory.
     *
     * This factory creates the listeners that consume messages from queues.
     * Tuned for high-throughput real-time processing with auto-scaling capabilities.
     *
     * @param connectionFactory RabbitMQ connection factory (auto-configured by Spring)
     * @param messageConverter JSON message converter
     * @return Configured listener container factory
     */
    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        messageConverter: MessageConverter
    ): SimpleRabbitListenerContainerFactory {
        return SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setMessageConverter(messageConverter)
            setConcurrentConsumers(MIN_CONCURRENT_CONSUMERS)
            setMaxConcurrentConsumers(MAX_CONCURRENT_CONSUMERS)
            setPrefetchCount(PREFETCH_COUNT)
        }
    }
}
