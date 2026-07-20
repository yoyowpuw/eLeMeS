package com.elemes.enrollment.api

import com.elemes.common.AuthzInput
import com.elemes.common.ForbiddenException
import com.elemes.common.OpaAuthorizer
import com.elemes.common.roles
import com.elemes.common.tenantId
import com.elemes.enrollment.Enrollment
import com.elemes.enrollment.EnrollmentStatus
import com.elemes.enrollment.infrastructure.CourseManagementClient
import com.elemes.enrollment.infrastructure.EnrollmentRepository
import com.elemes.enrollment.infrastructure.PathProgressService
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

data class EnrollLearnerRequest(val learnerId: String, val courseId: String, val orgUnitId: UUID? = null)
data class ProgressRequest(val percentComplete: Int)
data class EnrollmentResponse(
    val enrollmentId: UUID,
    val tenantId: String,
    val learnerId: String,
    val courseId: String,
    val contentVersionId: UUID,
    val orgUnitId: UUID?,
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
    private val authorizer: OpaAuthorizer,
    private val pathProgressService: PathProgressService,
) {

    @PostMapping
    fun enroll(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: EnrollLearnerRequest): ResponseEntity<EnrollmentResponse> {
        authorizer.check(AuthzInput("create_enrollment", jwt.tenantId().value, jwt.roles()))
        // Ch.5 ADR-005 / Ch.21 §7: the CURRENT version at enrollment time is
        // fetched once here and pinned for good — never re-queried later,
        // even if the course is republished before this learner finishes.
        // The caller's own token is relayed to Course Management (token-relay).
        val version = courseManagementClient.getCurrentVersion(request.courseId, jwt.tokenValue)
            ?: throw InvalidCourseException(request.courseId)
        val enrollment = Enrollment.enroll(
            UUID.randomUUID(), jwt.tenantId(), request.learnerId, request.courseId, version.versionId, request.orgUnitId,
        )
        repository.save(enrollment)
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment.toResponse())
    }

    @PostMapping("/{id}/start")
    fun start(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<EnrollmentResponse> =
        mutate(jwt, id) { it.start() }

    @PostMapping("/{id}/progress")
    fun progress(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: ProgressRequest,
    ): ResponseEntity<EnrollmentResponse> = mutate(jwt, id) { it.recordProgress(request.percentComplete) }

    /**
     * Ch.21 §2: the no-assessment completion path also drives path
     * advancement — an enrollment tagged with a `pathProgressId` (created
     * via `/path-enrollments`, never directly by a learner) auto-advances
     * to its next step, or — on the final step — attaches the realized step
     * sequence onto this completion event for Certification to pick up.
     * See PathProgressService.onEnrollmentCompleted's doc comment.
     */
    @PostMapping("/{id}/complete")
    fun complete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<EnrollmentResponse> {
        val enrollment = loadOrThrow(id)
        authorizer.check(AuthzInput("read_enrollment", jwt.tenantId().value, jwt.roles(), enrollment.tenantId.value))
        enrollment.complete()
        val pathContext = if (enrollment.status == EnrollmentStatus.COMPLETED) {
            pathProgressService.onEnrollmentCompleted(enrollment)
        } else null
        repository.save(enrollment, pathContext = pathContext)
        return ResponseEntity.ok(enrollment.toResponse())
    }

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<EnrollmentResponse> {
        val enrollment = loadOrThrow(id)
        authorizer.check(AuthzInput("read_enrollment", jwt.tenantId().value, jwt.roles(), enrollment.tenantId.value))
        return ResponseEntity.ok(enrollment.toResponse())
    }

    private fun mutate(jwt: Jwt, id: UUID, action: (Enrollment) -> Unit): ResponseEntity<EnrollmentResponse> {
        val enrollment = loadOrThrow(id)
        authorizer.check(AuthzInput("read_enrollment", jwt.tenantId().value, jwt.roles(), enrollment.tenantId.value))
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
    orgUnitId = orgUnitId,
    status = status.name,
    progressPercent = progressPercent,
)
