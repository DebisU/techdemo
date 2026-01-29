package com.verisure.techdemo.reporting.controller

import com.verisure.techdemo.models.LocationReport
import com.verisure.techdemo.models.ReportResponse
import com.verisure.techdemo.reporting.service.ReportService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@WebMvcTest(ReportController::class)
class ReportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var reportService: ReportService

    @Test
    fun `should return report`() {
        val report = ReportResponse(
            totalUsers = 10,
            totalEvents = 100,
            reports = listOf(
                LocationReport(
                    userId = "1",
                    totalLocations = 10,
                    lastLatitude = 40.7128,
                    lastLongitude = -74.0060,
                    lastUpdated = Instant.now()
                )
            )
        )
        
        whenever(reportService.generateReport()).thenReturn(report)
        
        mockMvc.get("/api/reports").andExpect {
            status { isOk() }
            jsonPath("$.totalUsers") { value(10) }
            jsonPath("$.totalEvents") { value(100) }
            jsonPath("$.reports[0].userId") { value("1") }
        }
    }

    @Test
    fun `should return 404 for non-existent user`() {
        whenever(reportService.getUserReport("unknown")).thenReturn(emptyList())
        
        mockMvc.get("/api/reports/unknown").andExpect {
            status { isNotFound() }
        }
    }
}
