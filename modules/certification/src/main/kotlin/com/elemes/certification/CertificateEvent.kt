package com.elemes.certification

import com.elemes.common.TenantId
import java.time.Instant
import java.util.UUID

sealed interface CertificateEvent {
    val certificateId: UUID
    val tenantId: TenantId
    val occurredAt: Instant
}

/**
 * Ch.5 ADR-005 / Ch.26 §2: captures learner, course, the exact content
 * version pinned at enrollment time (Ch.21 §7 — not "whatever's current
 * now"), score, and issuance time as an immutable record, digitally signed
 * (Ch.26 §3 ADR-043) rather than checksummed, so an exported certificate
 * remains verifiably authentic outside this platform's control. Realized
 * branch/step sequence within a multi-step path (also Ch.21 §7) still isn't
 * modeled — Learning Paths (Ch.21) don't exist as a concept yet, only flat
 * course enrollment does. Tracked as the next gap, not silently dropped.
 */
data class CertificateIssued(
    override val certificateId: UUID,
    override val tenantId: TenantId,
    val enrollmentId: UUID,
    val learnerId: String,
    val courseId: String,
    val contentVersionId: UUID,
    val score: Int?,
    val signature: String,
    override val occurredAt: Instant,
) : CertificateEvent

data class CertificateRevoked(
    override val certificateId: UUID,
    override val tenantId: TenantId,
    val reason: String,
    override val occurredAt: Instant = Instant.now(),
) : CertificateEvent
