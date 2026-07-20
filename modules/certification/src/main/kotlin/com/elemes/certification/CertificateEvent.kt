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
 * remains verifiably authentic outside this platform's control. When this
 * certificate closes out a Learning Path's final step, `pathId`/
 * `pathVersionId`/`realizedStepCourseIds` capture the Ch.21 §7 / Ch.26 Blue
 * Team addendum requirement — which branch was actually taken, not just
 * that *a* certificate was issued — and are part of the signed payload
 * (see CertificatePayload.canonical), so tampering with the realized
 * sequence after the fact is detectable exactly like tampering with score.
 */
data class CertificateIssued(
    override val certificateId: UUID,
    override val tenantId: TenantId,
    val enrollmentId: UUID,
    val learnerId: String,
    val courseId: String,
    val contentVersionId: UUID,
    /** Ch.19: the learner's org unit at enrollment time, if any — not part of the signed payload, purely an authorization attribute for manager-scoped revocation. */
    val orgUnitId: UUID? = null,
    val score: Int?,
    /** Null for a direct-course enrollment or a non-final path step (neither issues a path-aware certificate); set together, never independently. */
    val pathId: UUID? = null,
    val pathVersionId: UUID? = null,
    val realizedStepCourseIds: List<UUID>? = null,
    val signature: String,
    override val occurredAt: Instant,
) : CertificateEvent

data class CertificateRevoked(
    override val certificateId: UUID,
    override val tenantId: TenantId,
    val reason: String,
    override val occurredAt: Instant = Instant.now(),
) : CertificateEvent
