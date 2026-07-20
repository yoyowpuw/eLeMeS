package com.elemes.enrollment.infrastructure

import com.elemes.common.EventEnvelope
import com.elemes.enrollment.ContentCompleted
import com.elemes.enrollment.ContentProgressed
import com.elemes.enrollment.ContentStarted
import com.elemes.enrollment.EnrollmentEvent
import com.elemes.enrollment.GradingFailed
import com.elemes.enrollment.GradingPassed
import com.elemes.enrollment.GradingStarted
import com.elemes.enrollment.LearnerEnrolled
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EnrollmentEventMapper(private val objectMapper: ObjectMapper) {

    fun toEnvelope(event: EnrollmentEvent): EventEnvelope {
        val type = event::class.simpleName!!
        return EventEnvelope(
            eventId = UUID.randomUUID(),
            tenantId = event.tenantId,
            aggregateId = event.enrollmentId,
            aggregateType = "Enrollment",
            sequenceNumber = 0, // assigned by the store at append time
            eventType = type,
            payload = objectMapper.writeValueAsString(event),
            occurredAt = event.occurredAt,
        )
    }

    fun toDomainEvent(envelope: EventEnvelope): EnrollmentEvent = when (envelope.eventType) {
        "LearnerEnrolled" -> objectMapper.readValue(envelope.payload, LearnerEnrolled::class.java)
        "ContentStarted" -> objectMapper.readValue(envelope.payload, ContentStarted::class.java)
        "ContentProgressed" -> objectMapper.readValue(envelope.payload, ContentProgressed::class.java)
        "ContentCompleted" -> objectMapper.readValue(envelope.payload, ContentCompleted::class.java)
        "GradingStarted" -> objectMapper.readValue(envelope.payload, GradingStarted::class.java)
        "GradingPassed" -> objectMapper.readValue(envelope.payload, GradingPassed::class.java)
        "GradingFailed" -> objectMapper.readValue(envelope.payload, GradingFailed::class.java)
        else -> error("Unknown event type: ${envelope.eventType}")
    }
}
