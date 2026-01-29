package com.verisure.techdemo.reporting.config

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * RabbitMQ Consumer Configuration for Reporting Service.
 *
 * Configures the message consumer infrastructure for event persistence.
 * Tuned for batch database insertions rather than real-time processing.
 *
 * ## Queue Configuration
 * The reporting queue is durable and receives events via fanout binding
 * from the api-gateway's exchange configuration.
 *
 * ## Consumer Tuning (Persistence-Optimized)
 * Different from realtime-processor, this config prioritizes:
 * - **Higher Prefetch**: 100 messages (batching for DB efficiency)
 * - **Fewer Consumers**: 2-5 (database connections are the bottleneck)
 *
 * ## Performance Rationale
 * ```
 * Prefetch = 100: Batch DB inserts reduce connection overhead
 * Consumers = 2-5: Matches HikariCP pool size (10 connections)
 *                  2 consumers × 5 concurrent = 10 max connections
 * ```
 *
 * @see com.verisure.techdemo.gateway.config.RabbitMQConfig Producer-side configuration
 * @see com.verisure.techdemo.reporting.consumer.LocationEventConsumer Message consumer
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Configuration
class RabbitMQConfig {

    companion object {
        /**
         * Queue name for reporting/persistence processing.
         * 
         * Events in this queue are persisted to PostgreSQL for
         * historical analysis and report generation.
         */
        const val REPORTING_QUEUE = "location.reporting.queue"
        
        /** Minimum number of concurrent consumers. */
        private const val MIN_CONCURRENT_CONSUMERS = 2
        
        /** Maximum number of concurrent consumers. */
        private const val MAX_CONCURRENT_CONSUMERS = 5
        
        /** Number of messages to prefetch per consumer (batch-oriented). */
        private const val PREFETCH_COUNT = 100
    }

    /**
     * Declares the reporting queue with durability.
     *
     * Durable queues survive broker restarts. For reporting, durability
     * is critical to ensure no events are lost before persistence.
     *
     * @return A durable queue instance for event persistence
     */
    @Bean
    fun reportingQueue(): Queue {
        return Queue(REPORTING_QUEUE, true) // durable = true
    }

    /**
     * Configures JSON message converter for RabbitMQ.
     *
     * Uses Jackson for automatic serialization/deserialization.
     * Shared converter bean used by both producer and consumer sides.
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
     * Tuned for database persistence workloads:
     * - Higher prefetch count for batch efficiency
     * - Fewer consumers to match DB connection pool
     *
     * @param connectionFactory RabbitMQ connection factory (auto-configured)
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
