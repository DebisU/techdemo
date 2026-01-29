package com.verisure.techdemo.reporting

import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

/**
 * Reporting Service Application - Persistent Storage & Analytics.
 *
 * Third microservice in the Verisure location tracking system. Responsible for:
 * - **Event Persistence**: Consuming events from RabbitMQ and storing in PostgreSQL
 * - **Report Generation**: Providing aggregated analytics via REST API
 * - **Caching**: Using Redis to cache report results for performance
 *
 * ## Architecture Role
 * ```
 *                          ┌─────────────────────┐
 *                          │   Reporting Service │ ← YOU ARE HERE
 *                          │      :8082          │
 *                          └─────────┬───────────┘
 *                                    │
 *                    ┌───────────────┼───────────────┐
 *                    ▼               ▼               ▼
 *              [RabbitMQ]      [PostgreSQL]      [Redis]
 *              consumer         storage          caching
 * ```
 *
 * ## Responsibilities
 * - Consumes from `location.reporting.queue` (fanout binding)
 * - Persists all location events to PostgreSQL for historical analysis
 * - Exposes REST endpoints for generating reports
 * - Caches report results in Redis (TTL: 60s)
 *
 * ## Configuration
 * - **Port**: 8082
 * - **Database**: PostgreSQL via HikariCP (10 connections)
 * - **Cache**: Redis with Caffeine as fallback
 *
 * @see com.verisure.techdemo.gateway.ApiGatewayApplication Entry point for events
 * @see com.verisure.techdemo.realtime.RealtimeProcessorApplication Real-time sibling
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableRabbit
class ReportingServiceApplication

/**
 * Application entry point.
 *
 * Bootstraps the Spring Boot application with all auto-configurations
 * for JPA, RabbitMQ, Redis caching, and actuator endpoints.
 *
 * @param args Command-line arguments passed to the application
 */
fun main(args: Array<String>) {
    runApplication<ReportingServiceApplication>(*args)
}
