package com.elemes.certification.api

import com.elemes.certification.Certificate
import com.elemes.certification.CertificatePayload
import com.elemes.certification.infrastructure.CertificateRepository
import com.elemes.certification.infrastructure.LocalSigningService
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
import java.util.UUID

data class CertificateResponse(
    val certificateId: UUID,
    val enrollmentId: UUID,
    val learnerId: String,
    val courseId: String,
    val contentVersionId: UUID,
    val score: Int?,
    val status: String,
    val issuedAt: String,
    val signature: String,
)
data class VerifyResponse(val valid: Boolean)
data class RevokeRequest(val reason: String)

class CertificateNotFoundException(id: UUID) : RuntimeException("Certificate $id not found")
class NoCertificateForEnrollmentException(enrollmentId: UUID) : RuntimeException("No certificate issued for enrollment $enrollmentId")

@RestController
@RequestMapping("/api/v1/certificates")
class CertificateController(
    private val repository: CertificateRepository,
    private val signingService: LocalSigningService,
) {

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<CertificateResponse> = ResponseEntity.ok(loadOrThrow(id).toResponse())

    @GetMapping("/by-enrollment/{enrollmentId}")
    fun getByEnrollment(@PathVariable enrollmentId: UUID): ResponseEntity<CertificateResponse> {
        val certificateId = repository.findByEnrollmentId(enrollmentId) ?: throw NoCertificateForEnrollmentException(enrollmentId)
        return ResponseEntity.ok(loadOrThrow(certificateId).toResponse())
    }

    /** Ch.26 §6: independently verifiable without platform access, given only the public key. */
    @GetMapping("/{id}/verify")
    fun verify(@PathVariable id: UUID): ResponseEntity<VerifyResponse> {
        val certificate = loadOrThrow(id)
        val payload = CertificatePayload.canonical(
            certificate.certificateId, certificate.tenantId.value, certificate.enrollmentId, certificate.learnerId,
            certificate.courseId, certificate.contentVersionId, certificate.score, certificate.issuedAt,
        )
        return ResponseEntity.ok(VerifyResponse(signingService.verify(payload, certificate.signature)))
    }

    @PostMapping("/{id}/revoke")
    fun revoke(@PathVariable id: UUID, @RequestBody request: RevokeRequest): ResponseEntity<CertificateResponse> {
        val certificate = loadOrThrow(id)
        certificate.revoke(request.reason)
        repository.save(certificate)
        return ResponseEntity.ok(certificate.toResponse())
    }

    @GetMapping("/public-key")
    fun publicKey(): ResponseEntity<Map<String, String>> = ResponseEntity.ok(mapOf("publicKeyBase64" to signingService.publicKeyBase64))

    private fun loadOrThrow(id: UUID): Certificate = repository.findById(id) ?: throw CertificateNotFoundException(id)
}

private fun Certificate.toResponse() = CertificateResponse(
    certificateId, enrollmentId, learnerId, courseId, contentVersionId, score, status.name, issuedAt.toString(), signature,
)

@RestControllerAdvice
class CertificateExceptionHandler {

    @ExceptionHandler(CertificateNotFoundException::class, NoCertificateForEnrollmentException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))

    @ExceptionHandler(IllegalStateException::class)
    fun handleInvalid(ex: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (ex.message ?: "invalid request")))
}
