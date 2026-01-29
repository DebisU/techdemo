package com.verisure.techdemo.realtime

import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Real-time Processor Application - Stream Processing Service.
 *
 * This service consumes location events from RabbitMQ and performs real-time analytics:
 * - Stores recent locations in Redis (low-latency cache)
 * - Detects anomalies (unusual movement patterns, large distance jumps)
 * - Tracks user activity metrics
 * - Calculates movement velocity and patterns
 *
 * ## Architecture Role
 * ```
 * [RabbitMQ] → [Realtime Processor] → [Redis Cache]
 *                                   → [Anomaly Alerts]
 * ```
 *
 * ## Key Features
 * - **High Throughput**: Concurrent consumers with prefetch optimization
 * - **Low Latency**: Redis for sub-millisecond data access
 * - **Anomaly Detection**: Haversine formula for distance calculation
 *
 * ## Ports
 * - HTTP: 8081 (for actuator/health checks)
 * - Metrics: /actuator/prometheus
 *
 * ## Dependencies
 * - RabbitMQ: Message consumption
 * - Redis: State storage and caching
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 * @see com.verisure.techdemo.realtime.service.RealtimeAnalyticsService
 * @see com.verisure.techdemo.realtime.consumer.LocationEventConsumer
 */
@SpringBootApplication
@EnableRabbit
class RealtimeProcessorApplication

/**
 * Application entry point.
 *
 * @param args Command line arguments (supports Spring Boot externalized configuration)
 */
fun main(args: Array<String>) {
    runApplication<RealtimeProcessorApplication>(*args)
}
