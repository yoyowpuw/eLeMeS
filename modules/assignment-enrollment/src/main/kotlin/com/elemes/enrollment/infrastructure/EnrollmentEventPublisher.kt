package com.elemes.enrollment.infrastructure

import com.elemes.common.EnrollmentEventMessage
import com.elemes.common.EnrollmentEventTopics
import com.elemes.common.JdbcOutboxStore
import com.elemes.enrollment.Enrollment
import com.elemes.enrollment.EnrollmentEvent
import com.elemes.enrollment.GradingPassed
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

/**
 * Builds the Ch.11 §4 Published Language message and hands it to the
 * transactional outbox rather than sending to Kafka directly — see
 * AssessmentEventPublisher's doc comment for why. Enrollment's completion
 * events are what Certification is waiting for, so this is the specific gap
 * that used to risk a "completed but never certified" enrollment on a crash.
 */
@Component
class EnrollmentEventPublisher(
    private val outboxStore: JdbcOutboxStore,
    private val objectMapper: ObjectMapper,
) {
    fun enqueue(event: EnrollmentEvent, enrollment: Enrollment, pathContext: PathCompletionContext? = null) {
        val message = EnrollmentEventMessage(
            eventType = event::class.simpleName!!,
            enrollmentId = event.enrollmentId,
            tenantId = event.tenantId.value,
            learnerId = enrollment.learnerId,
            courseId = enrollment.courseId,
            contentVersionId = enrollment.contentVersionId,
            orgUnitId = enrollment.orgUnitId,
            score = (event as? GradingPassed)?.score,
            pathId = pathContext?.pathId,
            pathVersionId = pathContext?.pathVersionId,
            realizedStepCourseIds = pathContext?.realizedStepCourseIds,
            occurredAt = event.occurredAt,
        )
        outboxStore.enqueue(
            topic = EnrollmentEventTopics.ENROLLMENT_EVENTS,
            key = event.enrollmentId.toString(),
            payload = objectMapper.writeValueAsString(message),
        )
    }
}
