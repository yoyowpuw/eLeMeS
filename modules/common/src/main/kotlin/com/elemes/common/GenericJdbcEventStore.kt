package com.elemes.common

import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.util.UUID

/**
 * Ch.12 §5 event store, reusable across every compliance-critical-tier context
 * (Ch.11 §5) since they all share the same append-only table shape — see each
 * service's own Flyway migration for the concrete DDL. `eventsTable` is always
 * a fixed, code-defined constant (never user input), so templating it into SQL
 * here carries no injection risk.
 *
 * Originally lived only in assignment-enrollment; extracted here once
 * assessment needed the identical pattern (per the "don't abstract
 * prematurely" note left in the original implementation).
 */
class GenericJdbcEventStore(
    private val jdbcTemplate: JdbcTemplate,
    private val eventsTable: String,
) : EventStore {

    override fun append(aggregateId: UUID, aggregateType: String, expectedVersion: Long, events: List<EventEnvelope>) {
        events.forEachIndexed { index, envelope ->
            val sequenceNumber = expectedVersion + index + 1
            try {
                jdbcTemplate.update(
                    """
                    insert into $eventsTable
                        (event_id, tenant_id, aggregate_id, aggregate_type, sequence_number, event_type, payload, occurred_at)
                    values (?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                    """.trimIndent(),
                    envelope.eventId,
                    envelope.tenantId.value,
                    aggregateId,
                    aggregateType,
                    sequenceNumber,
                    envelope.eventType,
                    envelope.payload,
                    Timestamp.from(envelope.occurredAt),
                )
            } catch (ex: DuplicateKeyException) {
                // The (aggregate_id, sequence_number) unique constraint is the
                // optimistic-concurrency guard: someone else already wrote this version.
                throw EventStoreConcurrencyException(
                    "Concurrent modification detected for aggregate $aggregateId at sequence $sequenceNumber"
                )
            }
        }
    }

    override fun loadEvents(aggregateId: UUID): List<EventEnvelope> =
        jdbcTemplate.query(
            """
            select event_id, tenant_id, aggregate_id, aggregate_type, sequence_number, event_type, payload, occurred_at
            from $eventsTable
            where aggregate_id = ?
            order by sequence_number asc
            """.trimIndent(),
            { rs, _ ->
                EventEnvelope(
                    eventId = UUID.fromString(rs.getString("event_id")),
                    tenantId = TenantId(rs.getString("tenant_id")),
                    aggregateId = UUID.fromString(rs.getString("aggregate_id")),
                    aggregateType = rs.getString("aggregate_type"),
                    sequenceNumber = rs.getLong("sequence_number"),
                    eventType = rs.getString("event_type"),
                    payload = rs.getString("payload"),
                    occurredAt = rs.getTimestamp("occurred_at").toInstant(),
                )
            },
            aggregateId,
        )
}
