package com.elemes.enrollment.infrastructure

import com.elemes.common.AssessmentEventMessage
import com.elemes.common.AssessmentEventTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Consumes the Assessment context's Published Language (Ch.11 §4) and
 * translates each message into a call on Enrollment's own aggregate/vocabulary
 * (Ch.10 §4) — this is the anti-corruption boundary: Assessment's event names
 * and shape never leak past this class.
 */
@Component
class AssessmentEventListener(private val repository: EnrollmentRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [AssessmentEventTopics.ASSESSMENT_EVENTS])
    fun onAssessmentEvent(message: AssessmentEventMessage) {
        val enrollment = repository.findById(message.enrollmentId)
        if (enrollment == null) {
            log.warn("Received {} for unknown enrollment {}", message.eventType, message.enrollmentId)
            return
        }

        try {
            when (message.eventType) {
                "AssessmentSubmitted" -> enrollment.enterGrading(message.assessmentId)
                "AssessmentPassed" -> enrollment.passGrading(message.assessmentId, message.score ?: 0)
                "AssessmentFailed" -> enrollment.failGrading(message.assessmentId, message.score ?: 0)
                else -> return // AssessmentStarted/Graded don't change Enrollment's own state
            }
            repository.save(enrollment)
        } catch (ex: IllegalStateException) {
            // At-least-once Kafka delivery means this handler can be invoked more
            // than once for the same upstream event. A duplicate lands here as an
            // invalid-transition guard failure, which is the expected, harmless
            // shape of idempotent re-consumption — not an error to retry/alert on.
            log.info(
                "Ignoring {} for enrollment {} — already past that transition ({})",
                message.eventType, message.enrollmentId, ex.message,
            )
        }
    }
}
