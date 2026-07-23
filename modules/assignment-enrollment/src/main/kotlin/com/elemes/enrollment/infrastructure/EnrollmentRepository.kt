package com.elemes.enrollment.infrastructure

import com.elemes.common.EventStore
import com.elemes.common.ProcessedMessageRecord
import com.elemes.common.ProcessedMessageStore
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
    private val processedMessageStore: ProcessedMessageStore,
) {
    fun findById(enrollmentId: UUID): Enrollment? {
        val envelopes = eventStore.loadEvents(enrollmentId)
        if (envelopes.isEmpty()) return null
        return Enrollment.rehydrate(enrollmentId, envelopes.map(mapper::toDomainEvent))
    }

    /**
     * The still-active step-`Enrollment` for a Learning Path — what a
     * caller polling `PathProgress` actually wants to link into (see
     * `PathEnrollmentController.get()`).
     *
     * Filters on `status != 'COMPLETED'` rather than "most recently
     * updated" — `PathProgressService.onEnrollmentCompleted()` saves the
     * *next* step's enrollment before `EnrollmentController.complete()`
     * goes on to save the *original*, just-completed one, so the
     * original's `updated_at` ends up later than the next step's despite
     * being the older event. `order by updated_at desc limit 1` alone
     * picked the wrong (just-completed) row for exactly that reason —
     * caught by end-to-end path-progress testing, not by the type system.
     * At most one non-completed row exists per `pathProgressId` at a time,
     * so this is unambiguous.
     */
    fun findCurrentStepEnrollmentId(pathProgressId: UUID): UUID? =
        jdbcTemplate.query(
            "select enrollment_id from enrollment_projection where path_progress_id = ? and status != 'COMPLETED' order by updated_at desc limit 1",
            { rs, _ -> UUID.fromString(rs.getString("enrollment_id")) },
            pathProgressId,
        ).firstOrNull()

    /**
     * `processedMessage`, when given, is recorded in the same transaction as
     * the event-store append below — see ProcessedMessageStore's doc comment
     * for why this must stay atomic with the write it accompanies.
     * `pathContext`, when given, is threaded onto the EnrollmentEventMessage
     * for this save's completion event — see PathCompletionContext's doc
     * comment for why this, not a separate write, is how it reaches Kafka.
     */
    @Transactional
    fun save(enrollment: Enrollment, processedMessage: ProcessedMessageRecord? = null, pathContext: PathCompletionContext? = null) {
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
        uncommitted.forEach { publisher.enqueue(it, enrollment, pathContext) }
        enrollment.markCommitted()
        processedMessage?.let { processedMessageStore.markProcessed(it.messageId, it.tenantId, it.consumer) }
    }

    private fun updateProjection(enrollment: Enrollment) {
        jdbcTemplate.update(
            """
            insert into enrollment_projection
                (enrollment_id, tenant_id, learner_id, course_id, org_unit_id, path_progress_id, status, progress_percent, updated_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            enrollment.pathProgressId,
            enrollment.status.name,
            enrollment.progressPercent,
            Timestamp.from(Instant.now()),
        )
    }
}
