package com.elemes.orghierarchy.infrastructure

import com.elemes.common.JdbcOutboxStore
import com.elemes.common.OrgUnitEventMessage
import com.elemes.common.OrgUnitEventTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Ch.19 §4: publishes via the transactional outbox rather than directly to
 * Kafka — see EnrollmentEventPublisher's doc comment for why. Must be
 * called from inside the same `@Transactional` method as the owning
 * repository write (see OrgUnitRepository.create()/reparent()).
 */
@Component
class OrgUnitEventPublisher(
    private val outboxStore: JdbcOutboxStore,
    private val objectMapper: ObjectMapper,
) {
    fun enqueue(eventType: String, orgUnitId: UUID, tenantId: String, hierarchyType: String?) {
        val message = OrgUnitEventMessage(
            eventType = eventType,
            orgUnitId = orgUnitId,
            tenantId = tenantId,
            hierarchyType = hierarchyType,
            occurredAt = Instant.now(),
        )
        outboxStore.enqueue(
            topic = OrgUnitEventTopics.ORG_UNIT_EVENTS,
            key = orgUnitId.toString(),
            payload = objectMapper.writeValueAsString(message),
        )
    }
}
