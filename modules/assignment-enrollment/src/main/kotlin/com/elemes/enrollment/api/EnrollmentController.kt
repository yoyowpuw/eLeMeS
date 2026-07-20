package com.elemes.enrollment.api

import com.elemes.common.tenantId
import com.elemes.enrollment.Enrollment
import com.elemes.enrollment.infrastructure.CourseManagementClient
import com.elemes.enrollment.infrastructure.EnrollmentRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
    val contentVersionId: UUID,
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

    @PostMapping
    fun enroll(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: EnrollLearnerRequest): ResponseEntity<EnrollmentResponse> {
        // Ch.5 ADR-005 / Ch.21 §7: the CURRENT version at enrollment time is
        // fetched once here and pinned for good — never re-queried later,
        // even if the course is republished before this learner finishes.
        // The caller's own token is relayed to Course Management (token-relay).
        val version = courseManagementClient.getCurrentVersion(request.courseId, jwt.tokenValue)
            ?: throw InvalidCourseException(request.courseId)
        val enrollment = Enrollment.enroll(
            UUID.randomUUID(), jwt.tenantId(), request.learnerId, request.courseId, version.versionId,
        )
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

    // Ch.17 Authorization (not yet built) is what would verify the caller's
    // tenant/role matches this enrollment — Phase A only requires *a* valid
    // token (enforced globally by SecurityConfig), not tenant-scoped access.
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
    contentVersionId = contentVersionId,
    status = status.name,
    progressPercent = progressPercent,
)
