package com.elemes.course.api

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
class CourseController(private val repository: CourseRepository) {

    @PostMapping
    fun create(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: CreateCourseRequest): ResponseEntity<CourseResponse> {
        val tenantId = jwt.getClaimAsString("tenant_id") ?: error("JWT is missing the required tenant_id claim")
        val course = repository.create(UUID.randomUUID(), tenantId, request.code, request.title, request.initialContentHash)
        return ResponseEntity.status(HttpStatus.CREATED).body(course.toResponse())
    }

    // Ch.17 Authorization (not yet built) is what would verify the caller's
    // tenant matches the resource being read here — Phase A only establishes
    // *who* is calling, not *what tenant-scoped access* they should have.
    // Every endpoint below still requires a valid token (SecurityConfig),
    // just doesn't cross-check it against the resource's tenant yet.

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<CourseResponse> =
        ResponseEntity.ok(loadOrThrow(id).toResponse())

    /** Ch.12 §7: publishing a version never touches or invalidates prior versions. */
    @PostMapping("/{id}/versions")
    fun publishVersion(@PathVariable id: UUID, @RequestBody request: PublishVersionRequest): ResponseEntity<ContentVersionResponse> {
        val version = repository.publishNewVersion(id, request.contentHash) ?: throw CourseNotFoundException(id)
        return ResponseEntity.status(HttpStatus.CREATED).body(version.toResponse())
    }

    @GetMapping("/{id}/current-version")
    fun currentVersion(@PathVariable id: UUID): ResponseEntity<ContentVersionResponse> {
        loadOrThrow(id)
        val version = repository.findCurrentVersion(id) ?: throw CourseNotFoundException(id)
        return ResponseEntity.ok(version.toResponse())
    }

    /** Historical lookup — proves an old version is still reachable after being superseded. */
    @GetMapping("/{id}/versions/{versionId}")
    fun version(@PathVariable id: UUID, @PathVariable versionId: UUID): ResponseEntity<ContentVersionResponse> {
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
}
