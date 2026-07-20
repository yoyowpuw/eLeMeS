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
 */
class OutboxPoller(
    private val outboxStore: JdbcOutboxStore,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 500)
    fun publishPending() {
        outboxStore.fetchUnpublished(limit = 100).forEach { row ->
            try {
                kafkaTemplate.send(row.topic, row.key, row.payload).get()
                outboxStore.markPublished(row.id)
            } catch (ex: Exception) {
                log.warn("Failed to publish outbox row {} to topic {}, will retry next poll", row.id, row.topic, ex)
            }
        }
    }
}
