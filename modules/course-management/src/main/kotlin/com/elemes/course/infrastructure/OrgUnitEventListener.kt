package com.elemes.course.infrastructure

import com.elemes.common.OrgUnitEventMessage
import com.elemes.common.OrgUnitEventTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Ch.19 §3 ADR-032: invalidates OrgScopeCache on any org-unit change.
 * Consumed as a plain String and parsed manually — this service's default
 * Kafka consumer factory is already configured for plain String
 * deserialization (see application.yml), unlike Certification, which needs
 * a dedicated factory to avoid colliding with a JsonDeserializer default
 * type fixed for a different topic.
 */
@Component
class OrgUnitEventListener(
    private val cache: OrgScopeCache,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [OrgUnitEventTopics.ORG_UNIT_EVENTS])
    fun onOrgUnitEvent(payload: String) {
        val message = objectMapper.readValue(payload, OrgUnitEventMessage::class.java)
        log.info("Received {} for org unit {}, invalidating org-scope cache", message.eventType, message.orgUnitId)
        cache.invalidateAll()
    }
}
