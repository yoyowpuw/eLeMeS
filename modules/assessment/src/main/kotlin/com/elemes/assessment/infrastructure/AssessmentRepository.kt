package com.elemes.assessment.infrastructure

import com.elemes.assessment.Assessment
import com.elemes.common.EventStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class AssessmentRepository(
    private val eventStore: EventStore,
    private val mapper: AssessmentEventMapper,
    private val jdbcTemplate: JdbcTemplate,
    private val publisher: AssessmentEventPublisher,
) {
    fun findById(assessmentId: UUID): Assessment? {
        val envelopes = eventStore.loadEvents(assessmentId)
        if (envelopes.isEmpty()) return null
        return Assessment.rehydrate(assessmentId, envelopes.map(mapper::toDomainEvent))
    }

    @Transactional
    fun save(assessment: Assessment) {
        val uncommitted = assessment.uncommittedEvents
        if (uncommitted.isEmpty()) return

        val baseVersion = assessment.version - uncommitted.size
        eventStore.append(assessment.assessmentId, "Assessment", baseVersion, uncommitted.map(mapper::toEnvelope))
        updateProjection(assessment)
        // Transactional outbox (Ch.15 §7-adjacent): this insert commits or
        // rolls back atomically with the event-store append above, closing
        // the drop-on-crash gap the direct-publish version had.
        uncommitted.forEach(publisher::enqueue)
        assessment.markCommitted()
    }

    private fun updateProjection(assessment: Assessment) {
        jdbcTemplate.update(
            """
            insert into assessment_projection (assessment_id, tenant_id, enrollment_id, status, score, updated_at)
            values (?, ?, ?, ?, ?, ?)
            on conflict (assessment_id) do update set
                status = excluded.status,
                score = excluded.score,
                updated_at = excluded.updated_at
            """.trimIndent(),
            assessment.assessmentId,
            assessment.tenantId.value,
            assessment.enrollmentId,
            assessment.status.name,
            assessment.score,
            Timestamp.from(Instant.now()),
        )
    }
}
