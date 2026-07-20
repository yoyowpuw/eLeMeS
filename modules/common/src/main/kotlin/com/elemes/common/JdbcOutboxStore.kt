package com.elemes.common

import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Transactional outbox: `enqueue` must be called from inside the SAME
 * `@Transactional` method as the owning aggregate's event-store append, using
 * the same `JdbcTemplate`/`DataSource`. A commit then guarantees the message
 * WILL eventually reach Kafka (a poller will find it); a rollback guarantees
 * it never queues at all. This closes the "publish happens as a separate,
 * droppable step after commit" gap flagged in the first cut of Assessment's
 * and Enrollment's event publishing.
 */
class JdbcOutboxStore(
    private val jdbcTemplate: JdbcTemplate,
    private val outboxTable: String,
) {
    fun enqueue(topic: String, key: String, payload: String) {
        jdbcTemplate.update(
            "insert into $outboxTable (id, topic, message_key, payload, created_at) values (?, ?, ?, ?, ?)",
            UUID.randomUUID(), topic, key, payload, Timestamp.from(Instant.now()),
        )
    }

    fun fetchUnpublished(limit: Int): List<OutboxRow> =
        jdbcTemplate.query(
            """
            select id, topic, message_key, payload
            from $outboxTable
            where published_at is null
            order by created_at asc
            limit ?
            """.trimIndent(),
            { rs, _ ->
                OutboxRow(
                    id = UUID.fromString(rs.getString("id")),
                    topic = rs.getString("topic"),
                    key = rs.getString("message_key"),
                    payload = rs.getString("payload"),
                )
            },
            limit,
        )

    fun markPublished(id: UUID) {
        jdbcTemplate.update("update $outboxTable set published_at = ? where id = ?", Timestamp.from(Instant.now()), id)
    }
}

data class OutboxRow(val id: UUID, val topic: String, val key: String, val payload: String)
