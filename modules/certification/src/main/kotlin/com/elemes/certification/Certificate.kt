package com.elemes.certification

import com.elemes.common.EventSourcedAggregate
import com.elemes.common.TenantId
import java.time.Instant
import java.util.UUID

enum class CertificateStatus { ISSUED, REVOKED }

class Certificate private constructor(
    val certificateId: UUID,
) : EventSourcedAggregate<CertificateEvent>() {

    private var _tenantId: TenantId? = null
    val tenantId: TenantId get() = _tenantId ?: error("Certificate $certificateId has no events applied yet")

    lateinit var enrollmentId: UUID
        private set
    lateinit var learnerId: String
        private set
    lateinit var courseId: String
        private set
    var score: Int? = null
        private set
    lateinit var signature: String
        private set
    lateinit var issuedAt: Instant
        private set
    var status: CertificateStatus = CertificateStatus.ISSUED
        private set

    companion object {
        fun issue(
            certificateId: UUID,
            tenantId: TenantId,
            enrollmentId: UUID,
            learnerId: String,
            courseId: String,
            score: Int?,
            signature: String,
            issuedAt: Instant,
        ): Certificate {
            val certificate = Certificate(certificateId)
            certificate.raise(
                CertificateIssued(certificateId, tenantId, enrollmentId, learnerId, courseId, score, signature, issuedAt)
            )
            return certificate
        }

        fun rehydrate(certificateId: UUID, history: List<CertificateEvent>): Certificate {
            val certificate = Certificate(certificateId)
            certificate.loadFromHistory(history)
            return certificate
        }
    }

    /** Ch.41 §3: revocation is append-only — the original issuance record is never deleted or edited. */
    fun revoke(reason: String) {
        check(status == CertificateStatus.ISSUED) { "Cannot revoke certificate in status $status" }
        raise(CertificateRevoked(certificateId, tenantId, reason))
    }

    override fun apply(event: CertificateEvent) {
        when (event) {
            is CertificateIssued -> {
                _tenantId = event.tenantId
                enrollmentId = event.enrollmentId
                learnerId = event.learnerId
                courseId = event.courseId
                score = event.score
                signature = event.signature
                issuedAt = event.occurredAt
                status = CertificateStatus.ISSUED
            }
            is CertificateRevoked -> status = CertificateStatus.REVOKED
        }
    }
}
