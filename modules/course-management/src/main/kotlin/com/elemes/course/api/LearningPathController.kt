package com.elemes.course.api

import com.elemes.common.AuthzInput
import com.elemes.common.OpaAuthorizer
import com.elemes.common.roles
import com.elemes.course.LearningPath
import com.elemes.course.PathStep
import com.elemes.course.PathVersion
import com.elemes.course.infrastructure.LearningPathRepository
import com.elemes.course.infrastructure.OrgScopeCache
import com.elemes.course.infrastructure.UnknownCourseException
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

data class CreateLearningPathRequest(val name: String, val courseIds: List<UUID>, val orgUnitId: UUID? = null)
data class PublishPathVersionRequest(val courseIds: List<UUID>)
data class LearningPathResponse(val pathId: UUID, val tenantId: String, val name: String, val currentVersionId: UUID, val orgUnitId: UUID?)
data class PathStepResponse(val stepOrder: Int, val courseId: UUID)
data class PathVersionResponse(val pathId: UUID, val versionId: UUID, val versionNumber: Int, val steps: List<PathStepResponse>)

class LearningPathNotFoundException(id: UUID) : RuntimeException("Learning path $id not found")

/**
 * Ch.21: `LearningPath`/`PathStep` inlined into course-management rather than
 * a standalone 7th service — Ch.21 is Standard tier (not compliance-critical,
 * Ch.11 §5), Low data-sensitivity, and structurally this is just "ordered
 * Course references, versioned the same way ContentVersion is" — the same
 * reasoning that folded Question Bank (Ch.24) into Assessment rather than
 * standing up a service per bounded context regardless of size.
 */
@RestController
@RequestMapping("/api/v1/learning-paths")
class LearningPathController(
    private val repository: LearningPathRepository,
    private val authorizer: OpaAuthorizer,
    private val orgScopeCache: OrgScopeCache,
) {

    /** Same admin/manager + opt-in org-scoping shape as CourseController.create() — see its doc comment. */
    @PostMapping
    fun create(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: CreateLearningPathRequest): ResponseEntity<LearningPathResponse> {
        val tenantId = jwt.getClaimAsString("tenant_id") ?: error("JWT is missing the required tenant_id claim")
        val roles = jwt.roles()
        val callerOrgUnits = if (request.orgUnitId != null) resolveCallerScope(roles, jwt) else emptyList()
        authorizer.check(AuthzInput("create_learning_path", tenantId, roles, callerOrgUnits = callerOrgUnits, resourceOrgUnit = request.orgUnitId?.toString()))
        val path = repository.create(UUID.randomUUID(), tenantId, request.name, request.courseIds, request.orgUnitId)
        return ResponseEntity.status(HttpStatus.CREATED).body(path.toResponse())
    }

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<LearningPathResponse> {
        val path = loadOrThrow(id)
        authorizer.check(AuthzInput("read_course", jwt.getClaimAsString("tenant_id") ?: "", jwt.roles(), path.tenantId))
        return ResponseEntity.ok(path.toResponse())
    }

    @PostMapping("/{id}/versions")
    fun publishVersion(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: PublishPathVersionRequest,
    ): ResponseEntity<PathVersionResponse> {
        val path = loadOrThrow(id)
        val roles = jwt.roles()
        val callerOrgUnits = if (path.orgUnitId != null) resolveCallerScope(roles, jwt) else emptyList()
        authorizer.check(
            AuthzInput(
                "publish_path_version", jwt.getClaimAsString("tenant_id") ?: "", roles, path.tenantId,
                callerOrgUnits, path.orgUnitId?.toString(),
            )
        )
        val version = repository.publishNewVersion(id, request.courseIds) ?: throw LearningPathNotFoundException(id)
        val steps = repository.findStepsByVersionId(version.versionId)
        return ResponseEntity.status(HttpStatus.CREATED).body(version.toResponse(id, steps))
    }

    /** What assignment-enrollment calls synchronously to pin a version at path-enrollment time (mirrors Course's current-version endpoint). */
    @GetMapping("/{id}/current-version")
    fun currentVersion(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<PathVersionResponse> {
        val path = loadOrThrow(id)
        authorizer.check(AuthzInput("read_course", jwt.getClaimAsString("tenant_id") ?: "", jwt.roles(), path.tenantId))
        val withSteps = repository.findCurrentVersionWithSteps(id) ?: throw LearningPathNotFoundException(id)
        return ResponseEntity.ok(withSteps.version.toResponse(id, withSteps.steps))
    }

    /**
     * Historical lookup — mirrors CourseController's `/versions/{versionId}`.
     * Ch.21 §3 / ADR-034: a path version is pinned once at path-enrollment
     * time and never re-queried; assignment-enrollment calls this (not
     * current-version) on every subsequent step advancement so the pinned
     * version's steps stay reachable even after the path is republished
     * mid-flight.
     */
    @GetMapping("/{id}/versions/{versionId}")
    fun version(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): ResponseEntity<PathVersionResponse> {
        val path = loadOrThrow(id)
        authorizer.check(AuthzInput("read_course", jwt.getClaimAsString("tenant_id") ?: "", jwt.roles(), path.tenantId))
        val version = repository.findVersionById(versionId)?.takeIf { it.pathId == id } ?: throw LearningPathNotFoundException(id)
        val steps = repository.findStepsByVersionId(versionId)
        return ResponseEntity.ok(version.toResponse(id, steps))
    }

    private fun loadOrThrow(id: UUID): LearningPath = repository.findById(id) ?: throw LearningPathNotFoundException(id)

    private fun resolveCallerScope(roles: List<String>, jwt: Jwt): List<String> =
        if ("admin" !in roles && "manager" in roles) {
            val username = jwt.getClaimAsString("preferred_username") ?: jwt.subject
            orgScopeCache.getScope(username, jwt.tokenValue)
        } else {
            emptyList()
        }
}

private fun LearningPath.toResponse() = LearningPathResponse(pathId, tenantId, name, currentVersionId, orgUnitId)
private fun PathVersion.toResponse(pathId: UUID, steps: List<PathStep>) =
    PathVersionResponse(pathId, versionId, versionNumber, steps.map { PathStepResponse(it.stepOrder, it.courseId) })

@RestControllerAdvice
class LearningPathExceptionHandler {
    @ExceptionHandler(LearningPathNotFoundException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))

    @ExceptionHandler(UnknownCourseException::class, IllegalArgumentException::class)
    fun handleBadRequest(ex: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to (ex.message ?: "bad request")))
}
