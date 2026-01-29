package com.verisure.techdemo.reporting.controller

import com.verisure.techdemo.models.LocationReport
import com.verisure.techdemo.models.ReportResponse
import com.verisure.techdemo.reporting.service.ReportService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for Location Reports.
 *
 * Exposes HTTP endpoints for querying aggregated location analytics.
 * Results are cached via [ReportService] for improved performance.
 *
 * ## Endpoints
 * | Method | Path               | Description           |
 * |--------|--------------------|-----------------------|
 * | GET    | /api/reports       | Global report (cached)|
 * | GET    | /api/reports/{id}  | User-specific report  |
 * | GET    | /api/reports/health| Health check endpoint |
 *
 * ## Example Usage
 * ```bash
 * # Get global report
 * curl http://localhost:8082/api/reports
 *
 * # Get user-specific report
 * curl http://localhost:8082/api/reports/user123
 * ```
 *
 * ## Response Codes
 * - **200 OK**: Report generated successfully
 * - **404 Not Found**: User has no location data
 *
 * @property reportService Service for generating reports
 *
 * @see ReportService Business logic for report generation
 * @see ReportResponse Response DTO for global reports
 * @see LocationReport Individual user report data
 *
 * @author Verisure Tech Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService
) {
    private val logger = LoggerFactory.getLogger(ReportController::class.java)

    /**
     * Returns aggregated location report for all users.
     *
     * This endpoint is backed by Redis cache (60s TTL).
     * First call generates from database; subsequent calls hit cache.
     *
     * @return HTTP 200 with [ReportResponse] containing all user summaries
     */
    @GetMapping
    fun getReport(): ResponseEntity<ReportResponse> {
        logger.info("Report requested")
        val report = reportService.generateReport()
        return ResponseEntity.ok(report)
    }

    /**
     * Returns location report for a specific user.
     *
     * Not cached - queries database directly for each request.
     *
     * @param userId The unique identifier of the user
     * @return HTTP 200 with user report, or HTTP 404 if no data found
     */
    @GetMapping("/{userId}")
    fun getUserReport(@PathVariable userId: String): ResponseEntity<List<LocationReport>> {
        logger.info("Report requested for user: {}", userId)
        val report = reportService.getUserReport(userId)
        
        return if (report.isEmpty()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(report)
        }
    }

    /**
     * Simple health check endpoint.
     *
     * Used by container orchestration for liveness probes.
     * For comprehensive health info, use `/actuator/health`.
     *
     * @return HTTP 200 with status "UP"
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP"))
    }
}
