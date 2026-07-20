package com.elemes.enrollment

import com.elemes.enrollment.api.EnrollLearnerRequest
import com.elemes.enrollment.api.EnrollmentResponse
import com.elemes.enrollment.api.ProgressRequest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end proof that the golden path actually works: REST -> aggregate ->
 * Postgres event store -> projection, against a real Postgres via
 * Testcontainers (not H2) so the jsonb/uuid-specific SQL is genuinely exercised.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnrollmentControllerIntegrationTest {

    @LocalServerPort
    var port: Int = 0

    private val restTemplate = TestRestTemplate()

    private fun url(path: String) = "http://localhost:$port/api/v1/enrollments$path"

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("elemes")
            .withUsername("elemes")
            .withPassword("elemes_local_dev")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Test
    fun `enroll, start, progress and complete follows Ch5 §4 state machine`() {
        val enrolled = restTemplate.postForEntity(
            url(""), EnrollLearnerRequest(learnerId = "learner-1", courseId = "course-1"), EnrollmentResponse::class.java
        )
        assertEquals(HttpStatus.CREATED, enrolled.statusCode)
        val id = enrolled.body!!.enrollmentId
        assertEquals("ASSIGNED", enrolled.body!!.status)

        val started = restTemplate.postForEntity(url("/$id/start"), null, EnrollmentResponse::class.java)
        assertEquals("IN_PROGRESS", started.body!!.status)

        val progressed = restTemplate.postForEntity(
            url("/$id/progress"), ProgressRequest(percentComplete = 40), EnrollmentResponse::class.java
        )
        assertEquals(40, progressed.body!!.progressPercent)

        val completed = restTemplate.postForEntity(url("/$id/complete"), null, EnrollmentResponse::class.java)
        assertEquals("COMPLETED", completed.body!!.status)
        assertEquals(100, completed.body!!.progressPercent)
    }

    @Test
    fun `Ch37 ADR-060 highest-progress-wins - a lower progress report never regresses stored progress`() {
        val enrolled = restTemplate.postForEntity(
            url(""), EnrollLearnerRequest(learnerId = "learner-2", courseId = "course-1"), EnrollmentResponse::class.java
        )
        val id = enrolled.body!!.enrollmentId
        restTemplate.postForEntity(url("/$id/start"), null, EnrollmentResponse::class.java)

        restTemplate.postForEntity(url("/$id/progress"), ProgressRequest(percentComplete = 70), EnrollmentResponse::class.java)
        val regressed = restTemplate.postForEntity(
            url("/$id/progress"), ProgressRequest(percentComplete = 30), EnrollmentResponse::class.java
        )

        assertEquals(70, regressed.body!!.progressPercent)
    }

    @Test
    fun `completing an enrollment that was never started is rejected as an invalid transition`() {
        val enrolled = restTemplate.postForEntity(
            url(""), EnrollLearnerRequest(learnerId = "learner-3", courseId = "course-1"), EnrollmentResponse::class.java
        )
        val id = enrolled.body!!.enrollmentId

        val response = restTemplate.postForEntity(url("/$id/complete"), null, Map::class.java)
        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertTrue(response.body!!["error"].toString().contains("Cannot complete"))
    }
}
