package com.elemes.enrollment.api

import com.elemes.common.TenantId
import com.elemes.enrollment.Enrollment
import com.elemes.enrollment.infrastructure.CourseManagementClient
import com.elemes.enrollment.infrastructure.EnrollmentRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class EnrollLearnerRequest(val learnerId: String, val courseId: String)
data class ProgressRequest(val percentComplete: Int)
data class EnrollmentResponse(
    val enrollmentId: UUID,
    val tenantId: String,
    val learnerId: String,
    val courseId: String,
    val status: String,
    val progressPercent: Int,
)

class EnrollmentNotFoundException(id: UUID) : RuntimeException("Enrollment $id not found")
class InvalidCourseException(courseId: String) : RuntimeException("Course $courseId does not exist")

@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentController(
    private val repository: EnrollmentRepository,
    private val courseManagementClient: CourseManagementClient,
) {

    // Local dev stand-in: Ch.16's bought-CIAM auth isn't wired yet, so every
    // request is scoped to one hardcoded tenant until real auth/tenancy lands.
    private val defaultTenant = TenantId("default-tenant")

    @PostMapping
    fun enroll(@RequestBody request: EnrollLearnerRequest): ResponseEntity<EnrollmentResponse> {
        if (!courseManagementClient.courseExists(request.courseId)) {
            throw InvalidCourseException(request.courseId)
        }
        val enrollment = Enrollment.enroll(UUID.randomUUID(), defaultTenant, request.learnerId, request.courseId)
        repository.save(enrollment)
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment.toResponse())
    }

    @PostMapping("/{id}/start")
    fun start(@PathVariable id: UUID): ResponseEntity<EnrollmentResponse> = mutate(id) { it.start() }

    @PostMapping("/{id}/progress")
    fun progress(@PathVariable id: UUID, @RequestBody request: ProgressRequest): ResponseEntity<EnrollmentResponse> =
        mutate(id) { it.recordProgress(request.percentComplete) }

    @PostMapping("/{id}/complete")
    fun complete(@PathVariable id: UUID): ResponseEntity<EnrollmentResponse> = mutate(id) { it.complete() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<EnrollmentResponse> =
        ResponseEntity.ok(loadOrThrow(id).toResponse())

    private fun mutate(id: UUID, action: (Enrollment) -> Unit): ResponseEntity<EnrollmentResponse> {
        val enrollment = loadOrThrow(id)
        action(enrollment)
        repository.save(enrollment)
        return ResponseEntity.ok(enrollment.toResponse())
    }

    private fun loadOrThrow(id: UUID): Enrollment = repository.findById(id) ?: throw EnrollmentNotFoundException(id)
}

private fun Enrollment.toResponse() = EnrollmentResponse(
    enrollmentId = enrollmentId,
    tenantId = tenantId.value,
    learnerId = learnerId,
    courseId = courseId,
    status = status.name,
    progressPercent = progressPercent,
)
