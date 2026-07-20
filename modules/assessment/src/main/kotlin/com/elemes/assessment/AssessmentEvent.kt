package com.elemes.assessment

import com.elemes.common.TenantId
import java.time.Instant
import java.util.UUID

/** Ch.24 §2: a question is inline here, not sourced from a persistent, reusable bank yet. */
data class Question(
    val questionId: String,
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int,
)

/** Ch.5 §5 event inventory, Assessment context (Ch.11 #9). */
sealed interface AssessmentEvent {
    val assessmentId: UUID
    val tenantId: TenantId
    val enrollmentId: UUID
    val occurredAt: Instant
}

data class AssessmentStarted(
    override val assessmentId: UUID,
    override val tenantId: TenantId,
    override val enrollmentId: UUID,
    val courseId: String,
    val questions: List<Question>,
    val passingScore: Int,
    override val occurredAt: Instant = Instant.now(),
) : AssessmentEvent

data class AssessmentSubmitted(
    override val assessmentId: UUID,
    override val tenantId: TenantId,
    override val enrollmentId: UUID,
    val answers: Map<String, Int>,
    override val occurredAt: Instant = Instant.now(),
) : AssessmentEvent

data class AssessmentGraded(
    override val assessmentId: UUID,
    override val tenantId: TenantId,
    override val enrollmentId: UUID,
    val score: Int,
    override val occurredAt: Instant = Instant.now(),
) : AssessmentEvent

data class AssessmentPassed(
    override val assessmentId: UUID,
    override val tenantId: TenantId,
    override val enrollmentId: UUID,
    val score: Int,
    override val occurredAt: Instant = Instant.now(),
) : AssessmentEvent

data class AssessmentFailed(
    override val assessmentId: UUID,
    override val tenantId: TenantId,
    override val enrollmentId: UUID,
    val score: Int,
    override val occurredAt: Instant = Instant.now(),
) : AssessmentEvent
