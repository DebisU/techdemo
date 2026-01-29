package com.verisure.techdemo.gateway.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * RabbitMQ Configuration for the API Gateway.
 *
 * Defines the message broker topology for the location tracking system.
 * Uses a **Fanout Exchange** pattern to broadcast events to multiple consumers.
 *
 * ## Topology Diagram
 * ```
 *                              ┌─────────────────────────┐
 *                              │  location.realtime.queue │ → Realtime Processor
 *                              └─────────────────────────┘
 *  [API Gateway] → [location.exchange (FANOUT)]
 *                              ┌─────────────────────────┐
 *                              │ location.reporting.queue │ → Reporting Service
 *                              └─────────────────────────┘
 * ```
 *
 * ## Why Fanout Exchange?
 * - **Decoupling**: Publishers don't need to know about consumers
 * - **Scalability**: Easy to add new consumers without code changes
 * - **Reliability**: Each consumer has its own queue (independent processing)
 * - **Flexibility**: Different consumers can process at different speeds
 *
 * ## Queue Properties
 * - **Durable**: Queues survive broker restart
 * - **Non-exclusive**: Multiple consumers can connect
 * - **Non-auto-delete**: Queues persist even without consumers
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@Configuration
class RabbitMQConfig {

    companion object {
        /** Name of the fanout exchange for location events */
        const val LOCATION_EXCHANGE = "location.exchange"
        
        /** Queue for real-time processing (Redis, anomaly detection) */
        const val REALTIME_QUEUE = "location.realtime.queue"
        
        /** Queue for persistence and reporting (PostgreSQL) */
        const val REPORTING_QUEUE = "location.reporting.queue"
    }

    /**
     * Creates the fanout exchange for broadcasting location events.
     *
     * @return Durable, non-auto-delete fanout exchange
     */
    @Bean
    fun locationExchange(): FanoutExchange {
        return FanoutExchange(LOCATION_EXCHANGE, true, false)
    }

    /**
     * Creates the queue for real-time processing.
     *
     * @return Durable queue for the realtime-processor service
     */
    @Bean
    fun realtimeQueue(): Queue {
        return Queue(REALTIME_QUEUE, true)
    }

    /**
     * Creates the queue for persistence and reporting.
     *
     * @return Durable queue for the reporting-service
     */
    @Bean
    fun reportingQueue(): Queue {
        return Queue(REPORTING_QUEUE, true)
    }

    /**
     * Binds the realtime queue to the fanout exchange.
     *
     * @param realtimeQueue The realtime queue bean
     * @param locationExchange The fanout exchange bean
     * @return Binding configuration
     */
    @Bean
    fun realtimeBinding(realtimeQueue: Queue, locationExchange: FanoutExchange): Binding {
        return BindingBuilder.bind(realtimeQueue).to(locationExchange)
    }

    /**
     * Binds the reporting queue to the fanout exchange.
     *
     * @param reportingQueue The reporting queue bean
     * @param locationExchange The fanout exchange bean
     * @return Binding configuration
     */
    @Bean
    fun reportingBinding(reportingQueue: Queue, locationExchange: FanoutExchange): Binding {
        return BindingBuilder.bind(reportingQueue).to(locationExchange)
    }

    /**
     * Configures JSON message serialization using Jackson.
     *
     * @return Jackson-based message converter for JSON serialization
     */
    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    /**
     * Configures the RabbitTemplate with JSON serialization.
     *
     * @param connectionFactory The AMQP connection factory
     * @param messageConverter The JSON message converter
     * @return Configured RabbitTemplate for message publishing
     */
    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, messageConverter: MessageConverter): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        return template
    }
}
