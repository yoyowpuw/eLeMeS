package com.elemes.course.api

import com.elemes.course.Course
import com.elemes.course.infrastructure.CourseRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.UUID

data class CreateCourseRequest(val code: String, val title: String)
data class CourseResponse(val courseId: UUID, val tenantId: String, val code: String, val title: String)

class CourseNotFoundException(id: UUID) : RuntimeException("Course $id not found")

@RestController
@RequestMapping("/api/v1/courses")
class CourseController(private val repository: CourseRepository) {

    // Same local-dev stand-in as assignment-enrollment: no real tenancy/auth yet.
    private val defaultTenant = "default-tenant"

    @PostMapping
    fun create(@RequestBody request: CreateCourseRequest): ResponseEntity<CourseResponse> {
        val course = Course(UUID.randomUUID(), defaultTenant, request.code, request.title, Instant.now())
        repository.save(course)
        return ResponseEntity.status(HttpStatus.CREATED).body(course.toResponse())
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<CourseResponse> =
        ResponseEntity.ok((repository.findById(id) ?: throw CourseNotFoundException(id)).toResponse())
}

private fun Course.toResponse() = CourseResponse(courseId, tenantId, code, title)

@RestControllerAdvice
class CourseExceptionHandler {
    @ExceptionHandler(CourseNotFoundException::class)
    fun handleNotFound(ex: CourseNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))
}
