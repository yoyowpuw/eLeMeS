package com.elemes.enrollment.api

import com.elemes.common.AuthzInput
import com.elemes.common.OpaAuthorizer
import com.elemes.common.roles
import com.elemes.common.tenantId
import com.elemes.enrollment.infrastructure.EnrollmentRepository
import com.elemes.enrollment.infrastructure.PathProgressRepository
import com.elemes.enrollment.infrastructure.PathProgressService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

data class EnrollInPathRequest(val learnerId: String, val pathId: UUID, val orgUnitId: UUID? = null)
data class PathEnrollmentResponse(
    val pathProgressId: UUID,
    val pathId: UUID,
    val pathVersionId: UUID,
    val status: String,
    val currentStepEnrollmentId: UUID,
    val currentStepCourseId: String,
)
data class PathProgressResponse(
    val pathProgressId: UUID,
    val pathId: UUID,
    val pathVersionId: UUID,
    val status: String,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val realizedStepCourseIds: List<UUID>,
    /** Null only in the pathological case where the step-advancement write itself failed after PathProgress was created — see SiloProvisioner-style "no retry/saga" caveats elsewhere in this project for the same class of gap. */
    val currentStepEnrollmentId: UUID?,
)

class PathProgressNotFoundException(id: UUID) : RuntimeException("Path progress $id not found")

/**
 * Ch.21 §2: enrolling in a Learning Path creates a `PathProgress` row and
 * the first step's ordinary `Enrollment` in one call — every subsequent
 * step's enrollment is created automatically by `PathProgressService` as
 * prior steps complete (see its doc comment), never through this endpoint.
 */
@RestController
@RequestMapping("/api/v1/path-enrollments")
class PathEnrollmentController(
    private val pathProgressService: PathProgressService,
    private val pathProgressRepository: PathProgressRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val authorizer: OpaAuthorizer,
) {
    @PostMapping
    fun enroll(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: EnrollInPathRequest): ResponseEntity<PathEnrollmentResponse> {
        authorizer.check(AuthzInput("create_enrollment", jwt.tenantId().value, jwt.roles()))
        val (progress, firstEnrollment) = pathProgressService.startPath(jwt.tenantId(), request.learnerId, request.pathId, request.orgUnitId, jwt.tokenValue)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            PathEnrollmentResponse(
                pathProgressId = progress.pathProgressId,
                pathId = progress.pathId,
                pathVersionId = progress.pathVersionId,
                status = progress.status.name,
                currentStepEnrollmentId = firstEnrollment.enrollmentId,
                currentStepCourseId = firstEnrollment.courseId,
            )
        )
    }

    /** What the frontend polls to show live path progress and link into whichever step-`Enrollment` is currently active — see `EnrollmentRepository.findCurrentStepEnrollmentId()`. */
    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<PathProgressResponse> {
        val progress = pathProgressRepository.findById(id) ?: throw PathProgressNotFoundException(id)
        authorizer.check(AuthzInput("read_enrollment", jwt.tenantId().value, jwt.roles(), progress.tenantId))
        return ResponseEntity.ok(
            PathProgressResponse(
                pathProgressId = progress.pathProgressId,
                pathId = progress.pathId,
                pathVersionId = progress.pathVersionId,
                status = progress.status.name,
                currentStepIndex = progress.currentStepIndex,
                totalSteps = progress.stepPlan.size,
                realizedStepCourseIds = progress.realizedStepCourseIds,
                currentStepEnrollmentId = enrollmentRepository.findCurrentStepEnrollmentId(id),
            )
        )
    }
}

@RestControllerAdvice
class PathProgressExceptionHandler {
    @ExceptionHandler(PathProgressNotFoundException::class)
    fun handleNotFound(ex: PathProgressNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))
}
