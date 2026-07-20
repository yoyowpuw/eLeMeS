package com.elemes.common

import java.time.Instant
import java.util.UUID

/**
 * Raw, serialized form of a domain event as persisted in the append-only event
 * log (Ch.12 §5). Bounded contexts define their own typed event hierarchies and
 * map to/from this envelope — this type never knows about a specific context's
 * event classes.
 */
data class EventEnvelope(
    val eventId: UUID,
    val tenantId: TenantId,
    val aggregateId: UUID,
    val aggregateType: String,
    val sequenceNumber: Long,
    val eventType: String,
    val payload: String,
    val occurredAt: Instant,
)

/**
 * Append-only event store contract per Ch.12 §5: the event log is the system
 * of record for the compliance-critical tier. `expectedVersion` enforces
 * optimistic concurrency — a mismatch means another writer got there first.
 */
interface EventStore {
    fun append(aggregateId: UUID, aggregateType: String, expectedVersion: Long, events: List<EventEnvelope>)
    fun loadEvents(aggregateId: UUID): List<EventEnvelope>
}

class EventStoreConcurrencyException(message: String) : RuntimeException(message)
