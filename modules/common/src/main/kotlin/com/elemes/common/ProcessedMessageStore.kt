package com.elemes.common

import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * The inbox side of the outbox pattern already used for publishing
 * (`JdbcOutboxStore`): an explicit, persisted log of every message this
 * consumer has already processed, checked *before* acting on a message and
 * written *in the same transaction* as the business-logic side effect it
 * guards. This replaces "hope the domain model's own state happens to make
 * reprocessing a harmless no-op" (still true and still checked as
 * defense-in-depth — see each listener) with an explicit, deliberate
 * mechanism that works even for side effects with no natural
 * already-done check of their own.
 *
 * `markProcessed` must be called from inside the same `@Transactional`
 * method as the aggregate save it accompanies — same discipline as
 * `JdbcOutboxStore.enqueue`, for the same reason: a crash between "the
 * business logic committed" and "the message is marked processed" would
 * otherwise reopen the exact gap this exists to close. Like
 * `JdbcOutboxStore`, this class has no `@Transactional` of its own — it's
 * a plain `JdbcTemplate` call that participates in whatever transaction is
 * already active on the calling thread, same mechanism, same reason.
 */
class ProcessedMessageStore(
    private val jdbcTemplate: JdbcTemplate,
    private val table: String,
) {
    fun isProcessed(messageId: UUID): Boolean =
        jdbcTemplate.queryForObject(
            "select exists(select 1 from $table where message_id = ?)",
            Boolean::class.java,
            messageId,
        ) ?: false

    fun markProcessed(messageId: UUID, tenantId: String, consumer: String) {
        jdbcTemplate.update(
            "insert into $table (message_id, tenant_id, consumer, processed_at) values (?, ?, ?, ?)",
            messageId, tenantId, consumer, Timestamp.from(Instant.now()),
        )
    }
}

/** Passed down into a repository's own `@Transactional fun save(...)` so the dedup-log insert commits atomically with the aggregate write it accompanies — see [ProcessedMessageStore]'s doc comment. */
data class ProcessedMessageRecord(val messageId: UUID, val tenantId: String, val consumer: String)
