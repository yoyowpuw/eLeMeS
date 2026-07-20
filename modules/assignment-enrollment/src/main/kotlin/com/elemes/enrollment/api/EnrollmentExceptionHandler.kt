package com.elemes.enrollment.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class EnrollmentExceptionHandler {

    @ExceptionHandler(EnrollmentNotFoundException::class)
    fun handleNotFound(ex: EnrollmentNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))

    @ExceptionHandler(InvalidCourseException::class)
    fun handleInvalidCourse(ex: InvalidCourseException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to (ex.message ?: "invalid course")))

    @ExceptionHandler(IllegalStateException::class, IllegalArgumentException::class)
    fun handleInvalidTransition(ex: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (ex.message ?: "invalid request")))
}
