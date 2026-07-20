package com.elemes.course.api

import com.elemes.common.AuthzInput
import com.elemes.common.ForbiddenException
import com.elemes.common.OpaAuthorizer
import com.elemes.common.roles
import com.elemes.course.ContentVersion
import com.elemes.course.Course
import com.elemes.course.infrastructure.CourseRepository
import com.elemes.course.infrastructure.OrgHierarchyClient
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

data class CreateCourseRequest(val code: String, val title: String, val initialContentHash: String, val orgUnitId: UUID? = null)
data class PublishVersionRequest(val contentHash: String)
data class CourseResponse(val courseId: UUID, val tenantId: String, val code: String, val title: String, val currentVersionId: UUID, val orgUnitId: UUID?)
data class ContentVersionResponse(val versionId: UUID, val courseId: UUID, val versionNumber: Int, val contentHash: String)

class CourseNotFoundException(id: UUID) : RuntimeException("Course $id not found")
class ContentVersionNotFoundException(id: UUID) : RuntimeException("Content version $id not found")

@RestController
@RequestMapping("/api/v1/courses")
class CourseController(
    private val repository: CourseRepository,
    private val authorizer: OpaAuthorizer,
    private val orgHierarchyClient: OrgHierarchyClient,
) {

    /**
     * Ch.17 ADR-028: restricted to admin/manager — a learner authenticating
     * successfully is not enough to author content. Ch.19: if the request
     * targets a specific `orgUnitId`, a manager (not admin) must actually
     * manage that org unit or a descendant of one — a course with no
     * `orgUnitId` stays open to any manager, since org-scoping here is
     * opt-in, not a blanket restriction on course creation.
     */
    @PostMapping
    fun create(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: CreateCourseRequest): ResponseEntity<CourseResponse> {
        val tenantId = jwt.getClaimAsString("tenant_id") ?: error("JWT is missing the required tenant_id claim")
        val roles = jwt.roles()
        val callerOrgUnits = resolveCallerScope(roles, jwt.tokenValue)
        authorizer.check(AuthzInput("create_course", tenantId, roles, callerOrgUnits = callerOrgUnits, resourceOrgUnit = request.orgUnitId?.toString()))
        val course = repository.create(UUID.randomUUID(), tenantId, request.code, request.title, request.initialContentHash, request.orgUnitId)
        return ResponseEntity.status(HttpStatus.CREATED).body(course.toResponse())
    }

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<CourseResponse> {
        val course = loadOrThrow(id)
        authorizer.check(AuthzInput("read_course", jwt.getClaimAsString("tenant_id") ?: "", jwt.roles(), course.tenantId))
        return ResponseEntity.ok(course.toResponse())
    }

    /**
     * Ch.12 §7: publishing a version never touches or invalidates prior
     * versions. Also admin/manager-restricted, and Ch.19-scoped the same
     * way as creation: a manager must manage (or be an ancestor-manager of)
     * the course's own `orgUnitId`, if it has one.
     */
    @PostMapping("/{id}/versions")
    fun publishVersion(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: PublishVersionRequest,
    ): ResponseEntity<ContentVersionResponse> {
        val course = loadOrThrow(id)
        val roles = jwt.roles()
        val callerOrgUnits = resolveCallerScope(roles, jwt.tokenValue)
        authorizer.check(
            AuthzInput(
                "publish_course_version", jwt.getClaimAsString("tenant_id") ?: "", roles, course.tenantId,
                callerOrgUnits, course.orgUnitId?.toString(),
            )
        )
        val version = repository.publishNewVersion(id, request.contentHash) ?: throw CourseNotFoundException(id)
        return ResponseEntity.status(HttpStatus.CREATED).body(version.toResponse())
    }

    @GetMapping("/{id}/current-version")
    fun currentVersion(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<ContentVersionResponse> {
        val course = loadOrThrow(id)
        authorizer.check(AuthzInput("read_course", jwt.getClaimAsString("tenant_id") ?: "", jwt.roles(), course.tenantId))
        val version = repository.findCurrentVersion(id) ?: throw CourseNotFoundException(id)
        return ResponseEntity.ok(version.toResponse())
    }

    /** Historical lookup — proves an old version is still reachable after being superseded. */
    @GetMapping("/{id}/versions/{versionId}")
    fun version(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): ResponseEntity<ContentVersionResponse> {
        val course = loadOrThrow(id)
        authorizer.check(AuthzInput("read_course", jwt.getClaimAsString("tenant_id") ?: "", jwt.roles(), course.tenantId))
        val version = repository.findVersionById(versionId)?.takeIf { it.courseId == id } ?: throw ContentVersionNotFoundException(versionId)
        return ResponseEntity.ok(version.toResponse())
    }

    private fun loadOrThrow(id: UUID): Course = repository.findById(id) ?: throw CourseNotFoundException(id)

    /** Only resolved for "manager" (not "admin") callers — admin always stays tenant-wide, so there's no need to pay for the extra HTTP round-trip. */
    private fun resolveCallerScope(roles: List<String>, bearerToken: String): List<String> =
        if ("admin" !in roles && "manager" in roles) {
            orgHierarchyClient.myScope(bearerToken).map { it.toString() }
        } else {
            emptyList()
        }
}

private fun Course.toResponse() = CourseResponse(courseId, tenantId, code, title, currentVersionId, orgUnitId)
private fun ContentVersion.toResponse() = ContentVersionResponse(versionId, courseId, versionNumber, contentHash)

@RestControllerAdvice
class CourseExceptionHandler {
    @ExceptionHandler(CourseNotFoundException::class, ContentVersionNotFoundException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to (ex.message ?: "forbidden")))
}
