package com.elemes.common

import java.time.Instant
import java.util.UUID

/**
 * Ch.11 §4 Published Language: Enrollment's wire contract for every committed
 * event, consumed by Certification (on ContentCompleted/GradingPassed) today
 * and available for future consumers (e.g. Notification) without further
 * change to Enrollment itself.
 */
data class EnrollmentEventMessage(
    /** Stable per logical event, generated once at publish time — a redelivery of the same outbox row resends the same messageId, which is what ProcessedMessageStore dedups on. */
    val messageId: UUID = UUID.randomUUID(),
    val eventType: String,
    val enrollmentId: UUID,
    val tenantId: String,
    val learnerId: String,
    val courseId: String,
    /** Ch.5 ADR-005 / Ch.21 §7: the version pinned at enrollment time — Certification signs exactly this, not "whatever's current now." */
    val contentVersionId: UUID,
    /** Ch.19: the learner's org unit, if any — lets Certification scope manager-initiated revocation to their own subtree. */
    val orgUnitId: UUID? = null,
    val score: Int?,
    val occurredAt: Instant,
)

object EnrollmentEventTopics {
    const val ENROLLMENT_EVENTS = "enrollment-events"
}
