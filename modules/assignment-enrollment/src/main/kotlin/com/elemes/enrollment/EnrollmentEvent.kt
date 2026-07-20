package com.elemes.enrollment

import com.elemes.common.TenantId
import java.time.Instant
import java.util.UUID

/**
 * Ch.5 §5 event inventory, Enrollment context (Ch.11 #8). GradingStarted/
 * Passed/Failed are Enrollment's own vocabulary for reacting to the Assessment
 * context's published events (AssessmentSubmitted/Passed/Failed) consumed over
 * Kafka — per Ch.10 §4, a context's events describe changes to *its own*
 * state, never re-use another context's event names, even when reacting to them.
 */
sealed interface EnrollmentEvent {
    val enrollmentId: UUID
    val tenantId: TenantId
    val occurredAt: Instant
}

data class LearnerEnrolled(
    override val enrollmentId: UUID,
    override val tenantId: TenantId,
    val learnerId: String,
    val courseId: String,
    /** Ch.5 ADR-005 / Ch.21 §7: pinned at enrollment time, never re-queried — this is what the eventual certificate signs. */
    val contentVersionId: UUID,
    /** Ch.19: which org unit this learner belongs to, if any — opt-in, propagated downstream to Certification for manager-scoped authorization. */
    val orgUnitId: UUID? = null,
    override val occurredAt: Instant = Instant.now(),
) : EnrollmentEvent

data class ContentStarted(
    override val enrollmentId: UUID,
    override val tenantId: TenantId,
    override val occurredAt: Instant = Instant.now(),
) : EnrollmentEvent

data class ContentProgressed(
    override val enrollmentId: UUID,
    override val tenantId: TenantId,
    val percentComplete: Int,
    override val occurredAt: Instant = Instant.now(),
) : EnrollmentEvent

data class ContentCompleted(
    override val enrollmentId: UUID,
    override val tenantId: TenantId,
    override val occurredAt: Instant = Instant.now(),
) : EnrollmentEvent

data class GradingStarted(
    override val enrollmentId: UUID,
    override val tenantId: TenantId,
    val assessmentId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : EnrollmentEvent

data class GradingPassed(
    override val enrollmentId: UUID,
    override val tenantId: TenantId,
    val assessmentId: UUID,
    val score: Int,
    override val occurredAt: Instant = Instant.now(),
) : EnrollmentEvent

data class GradingFailed(
    override val enrollmentId: UUID,
    override val tenantId: TenantId,
    val assessmentId: UUID,
    val score: Int,
    override val occurredAt: Instant = Instant.now(),
) : EnrollmentEvent
