package com.elemes.certification.infrastructure

import com.elemes.common.OrgUnitEventMessage
import com.elemes.common.OrgUnitEventTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Ch.19 §3 ADR-032: invalidates OrgScopeCache on any org-unit change.
 * Consumed as a plain String and parsed manually (see
 * `orgUnitKafkaListenerContainerFactory` in CertificateConfig) rather than
 * via the default JsonDeserializer-based factory, which already has its
 * `spring.json.value.default.type` fixed to `EnrollmentEventMessage` for
 * the `enrollment-events` topic — a second topic with a different payload
 * type needs its own factory, not a shared one.
 */
@Component
class OrgUnitEventListener(
    private val cache: OrgScopeCache,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [OrgUnitEventTopics.ORG_UNIT_EVENTS], containerFactory = "orgUnitKafkaListenerContainerFactory")
    fun onOrgUnitEvent(payload: String) {
        val message = objectMapper.readValue(payload, OrgUnitEventMessage::class.java)
        log.info("Received {} for org unit {}, invalidating org-scope cache", message.eventType, message.orgUnitId)
        cache.invalidateAll()
    }
}
