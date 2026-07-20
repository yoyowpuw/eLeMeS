package com.elemes.assessment.infrastructure

import com.elemes.assessment.AssessmentEvent
import com.elemes.assessment.AssessmentFailed
import com.elemes.assessment.AssessmentGraded
import com.elemes.assessment.AssessmentPassed
import com.elemes.assessment.AssessmentStarted
import com.elemes.assessment.AssessmentSubmitted
import com.elemes.common.AssessmentEventMessage
import com.elemes.common.AssessmentEventTopics
import com.elemes.common.JdbcOutboxStore
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

/**
 * Builds the Ch.11 §4 Published Language message and hands it to the
 * transactional outbox (`JdbcOutboxStore`) rather than sending to Kafka
 * directly — `enqueue` is called from inside `AssessmentRepository.save`'s
 * `@Transactional` boundary, so this insert commits or rolls back atomically
 * with the event-store append it accompanies. Actual Kafka delivery is the
 * `OutboxPoller`'s job, decoupled from this request.
 */
@Component
class AssessmentEventPublisher(
    private val outboxStore: JdbcOutboxStore,
    private val objectMapper: ObjectMapper,
) {
    fun enqueue(event: AssessmentEvent) {
        val message = when (event) {
            is AssessmentStarted -> toMessage(event, "AssessmentStarted", null)
            is AssessmentSubmitted -> toMessage(event, "AssessmentSubmitted", null)
            is AssessmentGraded -> toMessage(event, "AssessmentGraded", event.score)
            is AssessmentPassed -> toMessage(event, "AssessmentPassed", event.score)
            is AssessmentFailed -> toMessage(event, "AssessmentFailed", event.score)
        }
        outboxStore.enqueue(
            topic = AssessmentEventTopics.ASSESSMENT_EVENTS,
            key = event.enrollmentId.toString(),
            payload = objectMapper.writeValueAsString(message),
        )
    }

    private fun toMessage(event: AssessmentEvent, type: String, score: Int?) = AssessmentEventMessage(
        eventType = type,
        assessmentId = event.assessmentId,
        enrollmentId = event.enrollmentId,
        tenantId = event.tenantId.value,
        score = score,
        occurredAt = event.occurredAt,
    )
}
