package com.elemes.assessment.infrastructure

import com.elemes.assessment.AssessmentEvent
import com.elemes.assessment.AssessmentFailed
import com.elemes.assessment.AssessmentGraded
import com.elemes.assessment.AssessmentPassed
import com.elemes.assessment.AssessmentStarted
import com.elemes.assessment.AssessmentSubmitted
import com.elemes.common.EventEnvelope
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AssessmentEventMapper(private val objectMapper: ObjectMapper) {

    fun toEnvelope(event: AssessmentEvent): EventEnvelope {
        val type = event::class.simpleName!!
        return EventEnvelope(
            eventId = UUID.randomUUID(),
            tenantId = event.tenantId,
            aggregateId = event.assessmentId,
            aggregateType = "Assessment",
            sequenceNumber = 0,
            eventType = type,
            payload = objectMapper.writeValueAsString(event),
            occurredAt = event.occurredAt,
        )
    }

    fun toDomainEvent(envelope: EventEnvelope): AssessmentEvent = when (envelope.eventType) {
        "AssessmentStarted" -> objectMapper.readValue(envelope.payload, AssessmentStarted::class.java)
        "AssessmentSubmitted" -> objectMapper.readValue(envelope.payload, AssessmentSubmitted::class.java)
        "AssessmentGraded" -> objectMapper.readValue(envelope.payload, AssessmentGraded::class.java)
        "AssessmentPassed" -> objectMapper.readValue(envelope.payload, AssessmentPassed::class.java)
        "AssessmentFailed" -> objectMapper.readValue(envelope.payload, AssessmentFailed::class.java)
        else -> error("Unknown event type: ${envelope.eventType}")
    }
}
