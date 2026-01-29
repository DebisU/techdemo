package com.verisure.techdemo.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * API Gateway Application - Entry point for the Verisure Location Tracking System.
 *
 * This service acts as the HTTP ingress point for all location data. It:
 * - Accepts location updates via REST API (POST /api/locations)
 * - Validates incoming requests
 * - Transforms requests into domain events
 * - Publishes events to RabbitMQ for downstream processing
 * - Returns 202 Accepted for async processing pattern
 *
 * ## Architecture Role
 * ```
 * [Mobile/IoT] → [API Gateway] → [RabbitMQ] → [Realtime Processor]
 *                                          → [Reporting Service]
 * ```
 *
 * ## Ports
 * - HTTP: 8080 (configurable via server.port)
 * - Metrics: /actuator/prometheus
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 * @see com.verisure.techdemo.gateway.controller.LocationController
 * @see com.verisure.techdemo.gateway.service.EventPublisher
 */
@SpringBootApplication
class ApiGatewayApplication

/**
 * Application entry point.
 *
 * @param args Command line arguments (supports Spring Boot externalized configuration)
 */
fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
