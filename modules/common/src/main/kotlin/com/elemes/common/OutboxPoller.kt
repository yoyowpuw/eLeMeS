package com.elemes.common

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled

/**
 * The other half of the transactional outbox: polls for rows the owning
 * service has committed but not yet published, and publishes them. Payloads
 * are pre-serialized JSON strings (see each service's event publisher), sent
 * via a plain String-valued KafkaTemplate — consumers' JsonDeserializer reads
 * the same bytes it always would, regardless of who wrote them onto the wire.
 *
 * Synchronous `.get()` on the send is a deliberate simplicity-over-throughput
 * choice at this scale: a failed publish leaves the row unpublished, and the
 * next poll retries it — at-least-once, not exactly-once, same as before,
 * but now durable across a crash between DB commit and Kafka publish.
 *
 * Ch.12 §2 silo tier: this runs on a `@Scheduled` background thread, which
 * has no per-request [TenantContext] the way `JdbcTemplate` calls inside an
 * HTTP request or Kafka listener do — so, unlike them, it can't just rely on
 * [TenantAwareDataSource] routing "wherever the current tenant happens to
 * be." Left alone, every poll would silently only ever see the pooled
 * cluster's outbox table — a SILO tenant's own outbox rows live in a
 * completely different physical database this thread would never even
 * connect to, so nothing published on its behalf would ever leave that
 * database. Fixed by explicitly polling once for the pooled cluster (no
 * tenant set) and once more per known silo tenant, setting
 * [TenantContext] around each so the same underlying `outboxStore` calls
 * route to the right physical database each time.
 */
class OutboxPoller(
    private val outboxStore: JdbcOutboxStore,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val siloRoutingClient: SiloRoutingClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 500)
    fun publishPending() {
        publishPendingFor(tenantId = null)
        siloRoutingClient.knownSiloTenantIds().forEach { tenantId ->
            TenantContext.set(tenantId)
            try {
                publishPendingFor(tenantId)
            } finally {
                TenantContext.clear()
            }
        }
    }

    private fun publishPendingFor(tenantId: String?) {
        outboxStore.fetchUnpublished(limit = 100).forEach { row ->
            try {
                kafkaTemplate.send(row.topic, row.key, row.payload).get()
                outboxStore.markPublished(row.id)
            } catch (ex: Exception) {
                log.warn("Failed to publish outbox row {} to topic {} (tenant {}), will retry next poll", row.id, row.topic, tenantId ?: "pooled", ex)
            }
        }
    }
}
