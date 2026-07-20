package com.elemes.assessment.api

import com.elemes.assessment.Assessment
import com.elemes.assessment.Question
import com.elemes.assessment.infrastructure.AssessmentRepository
import com.elemes.common.AuthzInput
import com.elemes.common.ForbiddenException
import com.elemes.common.OpaAuthorizer
import com.elemes.common.roles
import com.elemes.common.tenantId
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

data class QuestionRequest(val questionId: String, val text: String, val options: List<String>, val correctOptionIndex: Int)
data class StartAssessmentRequest(
    val enrollmentId: UUID,
    val courseId: String,
    val questions: List<QuestionRequest>,
    val passingScore: Int = 70,
)
data class SubmitAssessmentRequest(val answers: Map<String, Int>)
data class AssessmentResponse(val assessmentId: UUID, val enrollmentId: UUID, val status: String, val score: Int?)

class AssessmentNotFoundException(id: UUID) : RuntimeException("Assessment $id not found")

@RestController
@RequestMapping("/api/v1/assessments")
class AssessmentController(
    private val repository: AssessmentRepository,
    private val authorizer: OpaAuthorizer,
) {

    @PostMapping
    fun start(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: StartAssessmentRequest): ResponseEntity<AssessmentResponse> {
        authorizer.check(AuthzInput("create_assessment", jwt.tenantId().value, jwt.roles()))
        val questions = request.questions.map { Question(it.questionId, it.text, it.options, it.correctOptionIndex) }
        val assessment = Assessment.start(
            UUID.randomUUID(), jwt.tenantId(), request.enrollmentId, request.courseId, questions, request.passingScore
        )
        repository.save(assessment)
        return ResponseEntity.status(HttpStatus.CREATED).body(assessment.toResponse())
    }

    @PostMapping("/{id}/submit")
    fun submit(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: SubmitAssessmentRequest,
    ): ResponseEntity<AssessmentResponse> {
        val assessment = loadOrThrow(id)
        authorizer.check(AuthzInput("submit_assessment", jwt.tenantId().value, jwt.roles(), assessment.tenantId.value))
        assessment.submit(request.answers)
        repository.save(assessment)
        return ResponseEntity.ok(assessment.toResponse())
    }

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<AssessmentResponse> {
        val assessment = loadOrThrow(id)
        authorizer.check(AuthzInput("read_assessment", jwt.tenantId().value, jwt.roles(), assessment.tenantId.value))
        return ResponseEntity.ok(assessment.toResponse())
    }

    private fun loadOrThrow(id: UUID): Assessment = repository.findById(id) ?: throw AssessmentNotFoundException(id)
}

private fun Assessment.toResponse() = AssessmentResponse(assessmentId, enrollmentId, status.name, score)

@RestControllerAdvice
class AssessmentExceptionHandler {

    @ExceptionHandler(AssessmentNotFoundException::class)
    fun handleNotFound(ex: AssessmentNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to (ex.message ?: "forbidden")))

    @ExceptionHandler(IllegalStateException::class, IllegalArgumentException::class)
    fun handleInvalid(ex: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (ex.message ?: "invalid request")))
}
