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
 * Ch.5 ADR-005 / Ch.26 §2: captures learner, course, score, and issuance time
 * as an immutable record, digitally signed (Ch.26 §3 ADR-043) rather than
 * checksummed, so an exported certificate remains verifiably authentic
 * outside this platform's control. Content/path *version* pinning and the
 * realized branch/step sequence (Ch.21 §7) aren't modeled yet — Course
 * Management has no versioning of its own yet either — so `courseId` is the
 * closest current proxy. Tracked as a gap, not silently dropped.
 */
data class CertificateIssued(
    override val certificateId: UUID,
    override val tenantId: TenantId,
    val enrollmentId: UUID,
    val learnerId: String,
    val courseId: String,
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
