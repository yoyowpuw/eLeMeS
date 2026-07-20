package com.elemes.common

import java.time.Instant
import java.util.UUID

/**
 * Ch.11 §4 Published Language: the stable wire contract that Assessment
 * publishes and Enrollment consumes across the Kafka event bus (Ch.15
 * ADR-024). Deliberately not either service's internal domain-event type —
 * each side depends on this shared shape, not on the other's domain model.
 */
data class AssessmentEventMessage(
    /** Stable per logical event, generated once at publish time — a redelivery of the same outbox row resends the same messageId, which is what ProcessedMessageStore dedups on. */
    val messageId: UUID = UUID.randomUUID(),
    val eventType: String,
    val assessmentId: UUID,
    val enrollmentId: UUID,
    val tenantId: String,
    val score: Int?,
    val occurredAt: Instant,
)

object AssessmentEventTopics {
    const val ASSESSMENT_EVENTS = "assessment-events"
}
