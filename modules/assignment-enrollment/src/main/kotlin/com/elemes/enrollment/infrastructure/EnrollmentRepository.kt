package com.elemes.enrollment.infrastructure

import com.elemes.common.EventStore
import com.elemes.enrollment.Enrollment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Rehydrates aggregates by replaying the event log and keeps a synchronous
 * projection table for fast reads. Ch.12 §5's async/CDC projection style is a
 * later optimization once there's a second consumer of these events — a
 * same-transaction update is simpler and correct for a single-service slice.
 */
@Repository
class EnrollmentRepository(
    private val eventStore: EventStore,
    private val mapper: EnrollmentEventMapper,
    private val jdbcTemplate: JdbcTemplate,
    private val publisher: EnrollmentEventPublisher,
) {
    fun findById(enrollmentId: UUID): Enrollment? {
        val envelopes = eventStore.loadEvents(enrollmentId)
        if (envelopes.isEmpty()) return null
        return Enrollment.rehydrate(enrollmentId, envelopes.map(mapper::toDomainEvent))
    }

    @Transactional
    fun save(enrollment: Enrollment) {
        val uncommitted = enrollment.uncommittedEvents
        if (uncommitted.isEmpty()) return

        val baseVersion = enrollment.version - uncommitted.size
        eventStore.append(
            enrollment.enrollmentId,
            "Enrollment",
            baseVersion,
            uncommitted.map(mapper::toEnvelope),
        )
        updateProjection(enrollment)
        // Transactional outbox (Ch.15 §7-adjacent): commits or rolls back
        // atomically with the event-store append above.
        uncommitted.forEach { publisher.enqueue(it, enrollment) }
        enrollment.markCommitted()
    }

    private fun updateProjection(enrollment: Enrollment) {
        jdbcTemplate.update(
            """
            insert into enrollment_projection
                (enrollment_id, tenant_id, learner_id, course_id, org_unit_id, status, progress_percent, updated_at)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (enrollment_id) do update set
                status = excluded.status,
                progress_percent = excluded.progress_percent,
                updated_at = excluded.updated_at
            """.trimIndent(),
            enrollment.enrollmentId,
            enrollment.tenantId.value,
            enrollment.learnerId,
            enrollment.courseId,
            enrollment.orgUnitId,
            enrollment.status.name,
            enrollment.progressPercent,
            Timestamp.from(Instant.now()),
        )
    }
}
