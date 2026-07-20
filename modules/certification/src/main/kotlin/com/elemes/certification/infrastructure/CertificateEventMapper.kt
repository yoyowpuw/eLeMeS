package com.elemes.certification.infrastructure

import com.elemes.certification.CertificateEvent
import com.elemes.certification.CertificateIssued
import com.elemes.certification.CertificateRevoked
import com.elemes.common.EventEnvelope
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CertificateEventMapper(private val objectMapper: ObjectMapper) {

    fun toEnvelope(event: CertificateEvent): EventEnvelope {
        val type = event::class.simpleName!!
        return EventEnvelope(
            eventId = UUID.randomUUID(),
            tenantId = event.tenantId,
            aggregateId = event.certificateId,
            aggregateType = "Certificate",
            sequenceNumber = 0,
            eventType = type,
            payload = objectMapper.writeValueAsString(event),
            occurredAt = event.occurredAt,
        )
    }

    fun toDomainEvent(envelope: EventEnvelope): CertificateEvent = when (envelope.eventType) {
        "CertificateIssued" -> objectMapper.readValue(envelope.payload, CertificateIssued::class.java)
        "CertificateRevoked" -> objectMapper.readValue(envelope.payload, CertificateRevoked::class.java)
        else -> error("Unknown event type: ${envelope.eventType}")
    }
}
