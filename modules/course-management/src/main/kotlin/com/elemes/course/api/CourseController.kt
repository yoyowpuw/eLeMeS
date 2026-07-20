package com.elemes.course.api

import com.elemes.common.AuthzInput
import com.elemes.common.ForbiddenException
import com.elemes.common.OpaAuthorizer
import com.elemes.common.roles
import com.elemes.course.ContentVersion
import com.elemes.course.Course
import com.elemes.course.infrastructure.CourseRepository
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

data class CreateCourseRequest(val code: String, val title: String, val initialContentHash: String)
data class PublishVersionRequest(val contentHash: String)
data class CourseResponse(val courseId: UUID, val tenantId: String, val code: String, val title: String, val currentVersionId: UUID)
data class ContentVersionResponse(val versionId: UUID, val courseId: UUID, val versionNumber: Int, val contentHash: String)

class CourseNotFoundException(id: UUID) : RuntimeException("Course $id not found")
class ContentVersionNotFoundException(id: UUID) : RuntimeException("Content version $id not found")

@RestController
@RequestMapping("/api/v1/courses")
class CourseController(
    private val repository: CourseRepository,
    private val authorizer: OpaAuthorizer,
) {

    /** Ch.17 ADR-028: restricted to admin/manager — a learner authenticating successfully is not enough to author content. */
    @PostMapping
    fun create(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: CreateCourseRequest): ResponseEntity<CourseResponse> {
        val tenantId = jwt.getClaimAsString("tenant_id") ?: error("JWT is missing the required tenant_id claim")
        authorizer.check(AuthzInput("create_course", tenantId, jwt.roles()))
        val course = repository.create(UUID.randomUUID(), tenantId, request.code, request.title, request.initialContentHash)
        return ResponseEntity.status(HttpStatus.CREATED).body(course.toResponse())
    }

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<CourseResponse> {
        val course = loadOrThrow(id)
        authorizer.check(AuthzInput("read_course", jwt.getClaimAsString("tenant_id") ?: "", jwt.roles(), course.tenantId))
        return ResponseEntity.ok(course.toResponse())
    }

    /** Ch.12 §7: publishing a version never touches or invalidates prior versions. Also admin/manager-restricted. */
    @PostMapping("/{id}/versions")
    fun publishVersion(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: PublishVersionRequest,
    ): ResponseEntity<ContentVersionResponse> {
        val course = loadOrThrow(id)
        authorizer.check(AuthzInput("publish_course_version", jwt.getClaimAsString("tenant_id") ?: "", jwt.roles(), course.tenantId))
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
}

private fun Course.toResponse() = CourseResponse(courseId, tenantId, code, title, currentVersionId)
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
