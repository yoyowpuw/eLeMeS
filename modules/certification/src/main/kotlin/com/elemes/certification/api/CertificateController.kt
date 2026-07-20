package com.elemes.certification.api

import com.elemes.certification.Certificate
import com.elemes.certification.CertificatePayload
import com.elemes.certification.infrastructure.CertificateRepository
import com.elemes.certification.infrastructure.LocalSigningService
import com.elemes.certification.infrastructure.OrgScopeCache
import com.elemes.certification.infrastructure.OrgScopeUnavailableException
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

data class CertificateResponse(
    val certificateId: UUID,
    val enrollmentId: UUID,
    val learnerId: String,
    val courseId: String,
    val contentVersionId: UUID,
    val orgUnitId: UUID?,
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
    private val authorizer: OpaAuthorizer,
    private val orgScopeCache: OrgScopeCache,
) {

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<CertificateResponse> {
        val certificate = loadOrThrow(id)
        authorizer.check(AuthzInput("read_certificate", jwt.tenantId().value, jwt.roles(), certificate.tenantId.value))
        return ResponseEntity.ok(certificate.toResponse())
    }

    @GetMapping("/by-enrollment/{enrollmentId}")
    fun getByEnrollment(@AuthenticationPrincipal jwt: Jwt, @PathVariable enrollmentId: UUID): ResponseEntity<CertificateResponse> {
        val certificateId = repository.findByEnrollmentId(enrollmentId) ?: throw NoCertificateForEnrollmentException(enrollmentId)
        val certificate = loadOrThrow(certificateId)
        authorizer.check(AuthzInput("read_certificate", jwt.tenantId().value, jwt.roles(), certificate.tenantId.value))
        return ResponseEntity.ok(certificate.toResponse())
    }

    /**
     * Ch.26 §6: independently verifiable without platform access, given only
     * the public key — deliberately NOT gated by OPA/auth at all (see
     * SecurityConfig). No `@AuthenticationPrincipal` here since a caller may
     * have no token whatsoever.
     */
    @GetMapping("/{id}/verify")
    fun verify(@PathVariable id: UUID): ResponseEntity<VerifyResponse> {
        val certificate = loadOrThrow(id)
        val payload = CertificatePayload.canonical(
            certificate.certificateId, certificate.tenantId.value, certificate.enrollmentId, certificate.learnerId,
            certificate.courseId, certificate.contentVersionId, certificate.score, certificate.issuedAt,
        )
        return ResponseEntity.ok(VerifyResponse(signingService.verify(payload, certificate.signature)))
    }

    /**
     * Ch.17 ADR-028 / Ch.19: the highest-consequence mutation this service
     * exposes. `admin` can revoke any certificate in the tenant; `manager`
     * can only revoke ones whose learner belongs to an org unit the manager
     * actually manages (or a descendant of one) — resolved by asking
     * Org Hierarchy for the caller's own scope, token-relayed. Only resolved
     * when the caller isn't already admin, since admin never needs it — and
     * cached per Ch.19 ADR-032 (see OrgScopeCache) rather than re-fetched on
     * every single revoke call.
     */
    @PostMapping("/{id}/revoke")
    fun revoke(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: RevokeRequest,
    ): ResponseEntity<CertificateResponse> {
        val certificate = loadOrThrow(id)
        val roles = jwt.roles()
        val callerOrgUnits = if ("admin" !in roles && "manager" in roles) {
            val username = jwt.getClaimAsString("preferred_username") ?: jwt.subject
            orgScopeCache.getScope(username, jwt.tokenValue)
        } else {
            emptyList()
        }
        authorizer.check(
            AuthzInput(
                "revoke_certificate", jwt.tenantId().value, roles, certificate.tenantId.value,
                callerOrgUnits, certificate.orgUnitId?.toString(),
            )
        )
        certificate.revoke(request.reason)
        repository.save(certificate)
        return ResponseEntity.ok(certificate.toResponse())
    }

    @GetMapping("/public-key")
    fun publicKey(): ResponseEntity<Map<String, String>> = ResponseEntity.ok(mapOf("publicKeyBase64" to signingService.publicKeyBase64))

    private fun loadOrThrow(id: UUID): Certificate = repository.findById(id) ?: throw CertificateNotFoundException(id)
}

private fun Certificate.toResponse() = CertificateResponse(
    certificateId, enrollmentId, learnerId, courseId, contentVersionId, orgUnitId, score, status.name, issuedAt.toString(), signature,
)

@RestControllerAdvice
class CertificateExceptionHandler {

    @ExceptionHandler(CertificateNotFoundException::class, NoCertificateForEnrollmentException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "not found")))

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to (ex.message ?: "forbidden")))

    /** Explicit signal, not a generic 500 — org-hierarchy is unreachable and there's no cached scope to fall back to, so the decision can't be made at all. */
    @ExceptionHandler(OrgScopeUnavailableException::class)
    fun handleOrgScopeUnavailable(ex: OrgScopeUnavailableException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf("error" to (ex.message ?: "org scope unavailable")))

    @ExceptionHandler(IllegalStateException::class)
    fun handleInvalid(ex: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (ex.message ?: "invalid request")))
}
